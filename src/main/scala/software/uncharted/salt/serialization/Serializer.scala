package software.uncharted.salt.serialization

import software.uncharted.salt.core.generation.output.TileData

trait Serializer[V, X] {
  def serialize(tileData: TileData[_, V, X]): Array[Byte]
}
