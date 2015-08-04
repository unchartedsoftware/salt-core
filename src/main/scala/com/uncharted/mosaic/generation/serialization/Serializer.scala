package com.uncharted.mosiac.generation.serialization

import com.uncharted.mosiac.generation.TileBuilder
import com.uncharted.mosiac.generation.projection.Projection

trait Serializer[V, X] {
  def serialize(tileData: TileBuilder[_, _, V, _, X], projection: Projection, defaultValue: V): Array[Byte]
}
