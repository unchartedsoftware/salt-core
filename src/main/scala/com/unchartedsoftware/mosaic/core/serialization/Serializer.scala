package com.unchartedsoftware.mosaic.core.serialization

import com.unchartedsoftware.mosaic.core.projection.TileCoord
import com.unchartedsoftware.mosaic.core.generation.output.TileData

trait Serializer[TC <:TileCoord, V, X] {
  def serialize(tileData: TileData[TC, V, X]): Array[Byte]
}
