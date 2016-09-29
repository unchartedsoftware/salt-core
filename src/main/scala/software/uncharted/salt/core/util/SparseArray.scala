/*
 * Copyright 2016 Uncharted Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package software.uncharted.salt.core.util

import scala.reflect.ClassTag
import scala.collection.mutable.{ArrayLike, Builder, HashMap}

/**
 * An integer-indexed sparse array implementation, currently based on HashMap.
 * Specialized for storing Ints, Longs and Doubles
 * Automatically materializes into a dense array when the number of non-default
 * values stored exceeds some threshold.
 * Probably not thread-safe due to lack of locking on materialization.
 * TODO: https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/locks/ReadWriteLock.html
 *
 * @param size The maximum number of indices in the SparseArray
 * @param default The default value which will appear to be stored at "empty" positions in the SparseArray
 * @param inValues An initial set of values to place in the SparseArray. Should be fewer than size elements.
 * @param threshold when density() exceeds this threshold [0,1], this SparseArray will store its values in an
 *                  array instead of a map. 1/3 by default.
 * @tparam T the type of value being stored in the SparseArray
 */
class SparseArray[@specialized(Int, Long, Double) T] (
  size: Int,
  val default: T,
  inValues: Map[Int, T],
  threshold: Float = 1/3F
)(implicit tag: ClassTag[T])
  extends ArrayLike[T, SparseArray[T]]
  with Builder[T, SparseArray[T]]
  with Serializable {

  assert (size >= inValues.size)

  private var materialized = false
  private var internalSize = size
  private lazy val sparseValues = new HashMap[Int, T]() ++ inValues
  //scalastyle:off null
  private var denseValues: Array[T] = null
  //scalastyle:on null

  def this(size: Int, default: T)(implicit tag: ClassTag[T]) = {
    this(size, default, Map[Int, T]())
  }

  private def materialize() = {
    if (!materialized) {
      denseValues = Array.fill[T](internalSize)(default)
      for ((key, value) <- sparseValues) {
        denseValues.update(key, value)
      }
      materialized = true
      sparseValues.clear()
    }
  }

  @throws(classOf[ArrayIndexOutOfBoundsException])
  override def apply(idx: Int): T = {
    if (idx < internalSize) {
      if (materialized) {
        denseValues(idx)
      } else {
        sparseValues.getOrElse(idx, default)
      }
    } else {
      throw new ArrayIndexOutOfBoundsException(idx)
    }
  }

  override def length(): Int = {
    internalSize
  }

  /**
   * @return the ratio of concrete elements to the total virtual size of this SparseArray
   */
  def density(): Float = {
    if (materialized) {
      1
    } else {
      sparseValues.size / internalSize.toFloat
    }
  }

  @throws(classOf[ArrayIndexOutOfBoundsException])
  override def update(idx: Int, elem: T): Unit = {
    if (idx >= internalSize) {
      throw new ArrayIndexOutOfBoundsException(idx)
    } else if (materialized) {
      denseValues(idx) = elem
    } else if (!elem.equals(default)) {
      // only store non-default values
      sparseValues.put(idx, elem)
      // materialize sparse array into dense array if we're over the threshold
      if (this.density() > threshold) {
        this.materialize()
      }
    } else {
      // we know elem equals default, so wipe out the internally stored value if there was one
      sparseValues.remove(idx)
    }
  }

  override def seq: collection.IndexedSeq[T] = {
    if (materialized) {
      denseValues
    } else {
      val buff = Array.fill[T](internalSize)(default)
      for ((key, value) <- sparseValues) {
        buff(key) = value
      }
      buff
    }
  }

  override def newBuilder(): Builder[T, SparseArray[T]] = {
    new SparseArray(0, default)
  }

  //scalastyle:off method.name
  override def += (elem: T): this.type = {
  //scalastyle:on
    internalSize += 1
    // only store non-default values
    if (!elem.equals(default)) {
      sparseValues.put(internalSize-1, elem)
    }
    this
  }

  override def clear(): Unit = {
    internalSize = 0
    materialized = false
    //scalastyle:off null
    denseValues = null
    //scalastyle:on null
    sparseValues.clear()
  }

  override def result(): SparseArray[T] = {
    if (this.density() > threshold) {
      this.materialize()
    }
    this
  }
}
