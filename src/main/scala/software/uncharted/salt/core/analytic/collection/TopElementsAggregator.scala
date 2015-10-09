/*
 * Copyright 2015 Uncharted Software Inc.
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

import scala.collection.mutable.HashMap
import scala.collection.mutable.PriorityQueue
import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

/**
 * Aggregator for producing the top N common elements in a
 * collection column, along with their respective counts.
 *
 * @param elementLimit Produce up to this number of "top elements" in finish()
 * @tparam ET The element type
 */
class TopElementsAggregator[ET: ClassTag](elementLimit: Int)
extends Aggregator[Seq[ET], HashMap[ET, Int], List[(ET, Int)]] {

  def default(): HashMap[ET, Int] = {
    new HashMap[ET, Int]
  }

  override def add(current: HashMap[ET, Int], next: Option[Seq[ET]]): HashMap[ET, Int] = {
    if (next.isDefined) {
      next.get.foreach(t => {
        if (current.contains(t)) {
          current.put(t, current.get(t).get + 1)
        } else {
          current.put(t, 1)
        }
      })
    }
    current
  }

  override def merge(left: HashMap[ET, Int], right: HashMap[ET, Int]): HashMap[ET, Int] = {
    right.foreach(t => {
      left.put(t._1, left.getOrElse(t._1, 0) + t._2)
    })
    left
  }

  def finish(intermediate: HashMap[ET, Int]): List[(ET, Int)] = {
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
