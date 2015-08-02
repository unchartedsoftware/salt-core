package com.uncharted.mosiac.generation.serialization

import com.uncharted.mosiac.generation.TileAggregator
import com.uncharted.mosiac.generation.projection.Projection

trait Serializer[U, V] {
  def serialize(tileData: TileAggregator[_, U, V], projection: Projection, defaultValue: U): Array[Byte]
}
