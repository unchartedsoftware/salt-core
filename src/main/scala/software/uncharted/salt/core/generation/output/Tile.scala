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

package software.uncharted.salt.core.generation.output

import scala.collection.mutable.Map
import software.uncharted.salt.core.generation.Series

class Tile[TC](
  val coords: TC,
  private[salt] val seriesData: Map[String,SeriesData[TC,_,_]]
) extends Serializable {
  def apply[RT,DC,BC,T,U,V,W,X](series: Series[RT,DC,TC,BC,T,U,V,W,X]): Option[SeriesData[TC, V, X]] = {
    seriesData.get(series.id).asInstanceOf[Option[SeriesData[TC, V, X]]]
  }
}
