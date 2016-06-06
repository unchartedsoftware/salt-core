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

package software.uncharted.salt.core.analytic.collection

import software.uncharted.salt.core.analytic.Aggregator

import scala.collection.Map
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.{Map => MutableMap}
import scala.collection.mutable.PriorityQueue
import scala.reflect.ClassTag

/**
 * Aggregator for producing the top N common elements in a
 * collection column, along with their respective counts.
 *
 * @param elementLimit Produce up to this number of "top elements" in finish()
 * @tparam ET The element type
 */
class TopElementsAggregator[ET: ClassTag](elementLimit: Int)
extends Aggregator[Seq[ET], Map[ET, Int], List[(ET, Int)]] {

  def default(): Map[ET, Int] = {
    Map[ET, Int]()
  }

  override def add(current: Map[ET, Int], next: Option[Seq[ET]]): Map[ET, Int] = {
    if (next.isDefined) {
      // If our current map is mutable, add new data in directly.
      // If not, convert to a mutable map, and then add data in
      val sum = current match {
        case hm: MutableMap[ET, Int] => hm
        case _ => {
          // The current value isn't itself a mutable hashmap yet; convert to one.
          val hm = new HashMap[ET, Int]()
          hm ++= current
          hm
        }
      }
      next.get.foreach(t => sum.put(t, sum.getOrElse(t, 0) + 1))
      sum
    } else {
      current
    }
  }

  override def merge(left: Map[ET, Int], right: Map[ET, Int]): Map[ET, Int] = {
    // If either input map is mutable, merge the other into it.
    // If neither is, convert one to mutable, and add the other into it.
    val (to, from) = left match {
      case hm: MutableMap[ET, Int] => (hm, right)
      case _ =>
        right match {
          case hm: MutableMap[ET, Int] => (hm, left)
          case _ =>
            val hm = new HashMap[ET, Int]()
            hm ++= left
            (hm, right)
        }
    }
    from.foreach(t => {
      to.put(t._1, to.getOrElse(t._1, 0) + t._2)
    })
    to
  }

  override def finish(intermediate: Map[ET, Int]): List[(ET, Int)] = {
    val x = new PriorityQueue[(ET, Int)]()(Ordering.by(
      a => a._2
    ))
    intermediate.foreach(t => {
      x.enqueue(t)
    })
    var result = new ListBuffer[(ET, Int)]
    for (i <- 0 until Math.min(elementLimit, x.size)) {
      result.append(x.dequeue)
    }
    result.toList
  }
}
