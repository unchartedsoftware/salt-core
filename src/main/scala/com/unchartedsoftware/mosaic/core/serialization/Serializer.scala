package com.unchartedsoftware.mosaic.core.serialization

import com.unchartedsoftware.mosaic.core.generation.output.TileData

trait Serializer[V, X] {
  def serialize(tileData: TileData[_, V, X]): Array[Byte]
}
