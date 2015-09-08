package com.unchartedsoftware.mosaic.serialization

import com.unchartedsoftware.mosaic.core.generation.output.TileData

trait Serializer[V, X] {
  def serialize(tileData: TileData[_, V, X]): Array[Byte]
}
