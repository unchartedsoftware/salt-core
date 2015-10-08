package com.unchartedsoftware.salt.serialization

import com.unchartedsoftware.salt.core.generation.output.TileData

trait Serializer[V, X] {
  def serialize(tileData: TileData[_, V, X]): Array[Byte]
}
