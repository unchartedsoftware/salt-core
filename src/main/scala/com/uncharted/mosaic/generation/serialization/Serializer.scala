package com.uncharted.mosiac.generation.serialization

import com.uncharted.mosiac.generation.TileData

trait Serializer[V, X] {
  def serialize(tileData: TileData[V, X]): Array[Byte]
}
