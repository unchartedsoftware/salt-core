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



import scala.collection.mutable
import scala.reflect.ClassTag



/**
  * An integer-indexed sparse array implementation, currently based on HashMap.
  *
  * Specialized for storing Ints, Longs and Doubles
  *
  * Automatically materializes into a dense array when the number of non-default
  * values stored exceeds some threshold.
  *
  * Note that this is <em>not</em> a standard scala sequence.  While the map
  * method has basically the same meaning, the reduce method <em>only</em> has
  * the same meaning if the default value passed into the sparse array is a zero,
  * because reduce only operates on non-defaulted entries.  Also not that this
  * means that, if given a non-zero default value, reduce will return different
  * values if an array is materialized and if it is not.  In general, it is
  * clearly best to use a default value equivalent to zero with respect to any
  * operations that will be performed on the data.
  *
  * Probably not thread-safe due to lack of locking on materialization.
  *
  * TODO: https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/locks/ReadWriteLock.html
  *
  * @param _length The (fixed) length of the array
  * @param _default The default value used for elements without a specifically set value.  This value should always
  *                 be a zero with respect to the operations to be performed on the sparse array.  If it is not, the
  *                 above warnings about non-standard behavior in the reduce function will come into play.
  * @param _threshold The proportion of elements with non-default values beyond which the array will be materialized
  * @tparam T the type of value being stored in the SparseArray
  */
class SparseArray[@specialized(Int, Long, Double) T: ClassTag] (_length: Int, _default: => T, _threshold: Float = 1/3F
                                                               ) extends Serializable {
  private val sparseStorage = mutable.Map[Int, T]()
  private var denseStorage: Option[Array[T]] = None
  private val sampleDefault: T = _default

  /** Getter for the element at the given index */
  def apply (index: Int): T = {
    denseStorage.map { storage =>
      storage(index)
    }.getOrElse{
      sparseStorage.getOrElse(index,
        if (0 <= index && index < _length) {
          _default
        } else {
          throw new ArrayIndexOutOfBoundsException
        }
      )
    }
  }

  /** Another getter, for internal use only, that doesn't do bounds checks */
  private def boundlessApply (index: Int): T = {
    denseStorage.map { storage =>
      if (0 <= index && index < _length) storage(index) else _default
    }.getOrElse {
      sparseStorage.getOrElse(index, _default)
    }
  }

  /** Setter for the element at the given index */
  def update (index: Int, value: T): Unit = {
    if (denseStorage.isDefined) {
      denseStorage.foreach(_(index) = value)
    } else if (0 <= index && index < _length) {
      if (value == sampleDefault) {
        sparseStorage.remove(index)
      } else if (sparseStorage.contains(index) || sparseDensityWith(sparseStorage.size + 1) <= _threshold) {
        sparseStorage(index) = value
      } else {
        materialize()
        denseStorage.foreach(_(index) = value)
      }
    } else {
      throw new ArrayIndexOutOfBoundsException
    }
  }

  /** The (fixed) length of the sparse array */
  def length (): Int = _length
  /** The range of indices of the sparse array */
  def indices: Range = Range(0, _length)
  /** The default value of the sparse array */
  def default (): T = _default

  /** Change the sparse array to dense storage */
  private def materialize (): Unit = {
    if (denseStorage.isEmpty) {
      val newDenseStorage = Array.fill(_length)(_default)
      sparseStorage.foreach { case (index, value) => newDenseStorage(index) = value }
      denseStorage = Some(newDenseStorage)
      sparseStorage.clear()
    }
  }


  /** True if this SparseArray has been materialized */
  private[util] def isMaterialized = denseStorage.isDefined
  /** The proportion of possible elements having a non-default value beyond which the SparseArray will be
    * materialized
    */
  private[util] def materializationThreshold = _threshold

  /** Determine the density this array would have, if not materialized, and with the given number of elements having
    * non-default values.
    */
  private def sparseDensityWith (fillRate: Int): Float = fillRate.toFloat / _length
  /** The density - or the proportion of elements with non-default values - of this SparseArray */
  def density (): Float = {
    denseStorage.map(storage => 1.0f).getOrElse(sparseDensityWith(sparseStorage.size))
  }

  /** Transform this SparseArray according to the input function
    *
    * Note that side-effects (such as a side total) are <em>not</em> guaranteed correct in SparseArrays - the
    * transformation function will <em>not</em> be run on defaulted entries.
    *
    * @param fcn The value transformation function
    * @tparam U The output value type
    * @return A new SparseArray, with the values of this array transformed as specified
    */
  def map[U: ClassTag] (fcn: T => U): SparseArray[U] = {
    val result = new SparseArray(_length, fcn(_default), _threshold)

    if (isMaterialized) {
      // Materialization is just defining dense storage, so we don't need to do anything else here.
      result.denseStorage = denseStorage.map(_.map(fcn))
    } else {
      sparseStorage.foreach { case (index, value) =>
          result(index) = fcn(value)
      }
    }
    result
  }

  /** Transform this SparseArray according to the input function.
    *
    * Unlike map, the input function here is given the index of the element.
    *
    * For the default value, it is given an index of -1.
    *
    * @param fcn The value transformation function
    * @tparam U The output value type
    * @return A new SparseArray, with the values of  this array transformed as specified
    */
  def mapWithIndex[U: ClassTag] (fcn: (T, Int) => U): SparseArray[U] = {
    val result = new SparseArray(_length, fcn(default, -1), _threshold)

    if (isMaterialized) {
      // Materialization is just defining dense storage, so we don't need to do anything else here.
      result.denseStorage = denseStorage.map { storage =>
        storage.zipWithIndex.map{ case (value, index) => fcn(value, index)}
      }
    } else {
      sparseStorage.foreach { case (index, value) =>
        result(index) = fcn(value, index)
      }
    }
    result
  }

  /** Aggregate the non-defaulted values in this SparseArray
    *
    * Note there is a slight difference between how reduce runs in SparseArrays and in other sequences, in the the
    * reduction function is <em>only</em> run on non-defaulted values.  If running on defaulted values is desired,
    * please use seq.reduce.
    *
    * @param fcn A function that takes two values and combines them into one
    * @tparam U The reduced output type
    * @return The reduced value of all non-default entries in the SparseArray
    */
  def reduce[U >: T] (fcn: (U, U) => U): U = {
    denseStorage.map(_.reduce(fcn)).getOrElse(sparseStorage.values.reduce(fcn))
  }

  /** Transform this SparseArray into a normal scala Seq.  This returns a materialized form of the SparseArray, but
    * does not materialize the SparseArray itself.
    */
  def seq: Seq[T] = {
    denseStorage.map(_.toSeq).getOrElse {
      for (i <- 0 until _length) yield sparseStorage.getOrElse(i, _default)
    }
  }
}
object SparseArray {
  def apply[T: ClassTag] (length: Int, default: => T, threshold: Float = 1/3F)
                         (initialValues: (Int, T)*): SparseArray[T] = {
    val result = new SparseArray(length, default, threshold)
    initialValues.foreach { case (index, value) => result(index) = value }
    result
  }

  /** Merge two sparse arrays.
    *
    * @param fcn A function to merge individual values of the input sparse arrays
    * @param newMaterializationThreshold If defined, the materialization threshold to use in the new array.  If not
    *                                    defined, the higher of the materialization thresholds of the two input arrays
    *                                    is used.
    *                                    Note that if both input sparse arrays are materialized, the output will be
    *                                    materialized no matter what threshold is used.
    * @param a The first sparse array to merge
    * @param b The second sparse array to merge
    * @tparam A The type of the first sparse array
    * @tparam B The type of the second sparse array
    * @tparam C The type of the returned sparse array
    * @return The merged arrays
    */
  def merge [A, B, C: ClassTag] (fcn: (A, B) => C, newMaterializationThreshold: Option[Float] = None)(a: SparseArray[A], b: SparseArray[B]): SparseArray[C] = {
    val newLength = a.length max b.length
    val result = new SparseArray[C](
      a.length max b.length,
      fcn(a.default, b.default),
      newMaterializationThreshold.getOrElse(a.materializationThreshold max b.materializationThreshold)
    )

    def needed[D] (index: Int, array: SparseArray[D]): Boolean = {
      if (array.isMaterialized) {
        index < array.length
      } else {
        array.sparseStorage.contains(index)
      }
    }

    for (i <- 0 until newLength) {
      if (needed(i, a) || needed(i, b)) {
        result(i) = fcn(a.boundlessApply(i), b.boundlessApply(i))
      }
    }

    result
  }
}
