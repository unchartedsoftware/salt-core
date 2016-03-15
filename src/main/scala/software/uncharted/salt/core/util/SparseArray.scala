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
 * Specialized for storing Ints, Longs and Doubles.ss
 *
 * @param size The maximum number of indices in the SparseArray
 * @param default The default value which will appear to be stored at "empty" positions in the SparseArray
 * @param values An initial set of values to place in the SparseArray. Should be fewer than size elements.
 * @tparam T the type of value being stored in the SparseArray
 */
private[salt] class SparseArray[@specialized(Int, Long, Double) T](
  size: Int,
  default: T,
  values: HashMap[Int, T]
)(implicit tag: ClassTag[T])
  extends ArrayLike[T, SparseArray[T]]
  with Builder[T, SparseArray[T]]
  with Serializable {

  assert (size >= values.size)

  private var internalSize = size

  def this(size: Int, default: T)(implicit tag: ClassTag[T]) = {
    this(size, default, new HashMap[Int, T]())
  }

  override def apply(idx: Int): T = {
    values.getOrElse(idx, default)
  }

  override def length(): Int = {
    internalSize
  }

  override def update(idx: Int, elem: T): Unit = {
    values.put(idx, elem)
  }

  override def seq: collection.IndexedSeq[T] = {
    val buff = Array.fill[T](internalSize)(default)
    for ((key, value) <- values) {
      buff(key) = value
    }
    buff
  }

  override def newBuilder(): Builder[T, SparseArray[T]] = {
    new SparseArray(0, default)
  }

  //scalastyle:off method.name
  override def += (elem: T): this.type = {
  //scalastyle:on
    values.put(internalSize, elem)
    internalSize += 1
    this
  }

  override def clear(): Unit = {
    internalSize = 0
    values.clear()
  }

  override def result(): SparseArray[T] = {
    this
  }
}
