package com.uncharted.mosaic.generation.serialization

import com.uncharted.mosaic.generation.TileData

trait Serializer[V, X] {
  def serialize(tileData: TileData[V, X]): Array[Byte]
}
