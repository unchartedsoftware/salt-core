package com.uncharted.mosaic.generation.serialization

import java.io.{ByteArrayOutputStream, IOException, OutputStream}

import com.uncharted.mosaic.generation.serialization.AvroSchemaComposer
import com.uncharted.mosaic.generation.serialization.PatternedSchemaStore
import com.uncharted.mosaic.generation.TileData
import com.uncharted.mosaic.generation.analytic.numeric.MaxMinAggregator
import com.uncharted.mosaic.generation.projection.Projection
import org.apache.avro.Schema
import org.apache.avro.file.{CodecFactory, DataFileWriter}
import org.apache.avro.generic.{GenericData, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.DatumWriter
import scala.collection.JavaConversions._

/**
 * Suitable for serializing tiles where values are primitive values
 */
class PrimitiveTypeAvroSerializer[V, X](val tileDataType: Class[_ <: V], val maxBinCount: Int) extends Serializer[V, X] {

  private val VALID_PRIMITIVE_TYPES = Map[Class[_], String](
    classOf[java.lang.Boolean] -> "boolean",
    classOf[java.lang.Integer] -> "int",
    classOf[java.lang.Long] -> "long",
    classOf[java.lang.Float] -> "float",
    classOf[java.lang.Double] -> "double",
    classOf[java.lang.String] -> "string"
  )

  private val __schemaStore: PatternedSchemaStore  = new PatternedSchemaStore(
    "{\n" +
      "  \"name\":\"recordType\",\n" +
      "  \"namespace\":\"ar.avro\",\n" +
      "  \"type\":\"record\",\n" +
      "  \"fields\":[\n" +
      "    {\"name\":\"value\", \"type\":\"%s\"}\n" +
      "  ]\n" +
      "}")

  /**
   * Determines if a class represents a valid Avro primitive type
   */
  private def isValidPrimitive(candidateType: Class[_ <: V]): Boolean = {
    VALID_PRIMITIVE_TYPES.contains(candidateType)
  }

  /**
   * Get the avro string type of a valid primitive type
   */
  def getAvroType(key: Class[_ <: V]): Option[String] = {
    VALID_PRIMITIVE_TYPES.get(key)
  }

  //TODO make this abstract
  def setValue(valueRecord: GenericRecord, value: V) = {
    valueRecord.put("value", value)
  }

  def getTileMetaData(tileData: TileData[V, X]): java.util.Map[String, String] = {
    mapAsJavaMap(tileData.tileMeta match {
      case m: (_, _) => Map("minimum" -> m._1.toString, "maximum" -> m._2.toString)
      case _ => Map()
    })
  }

  private def descriptionToCodec(codecDescription: String): CodecFactory = {
    if (codecDescription.startsWith("deflate")) {
      val deflateLevel: Int = Integer.parseInt(codecDescription.substring(8))
      return CodecFactory.deflateCodec(deflateLevel)
    }
    else {
      return CodecFactory.fromString(codecDescription)
    }
  }

  private def codecToDescription(codec: CodecFactory): String = {
    return codec.toString
  }

  private def writeRecord(record: GenericRecord, schema: Schema, stream: OutputStream) {
    val datumWriter: DatumWriter[GenericRecord] = new GenericDatumWriter[GenericRecord](schema)
    val dataFileWriter: DataFileWriter[GenericRecord] = new DataFileWriter[GenericRecord](datumWriter)
    try {
      dataFileWriter.setCodec(CodecFactory.bzip2Codec())
      dataFileWriter.create(schema, stream)
      dataFileWriter.append(record)
      dataFileWriter.close
      stream.close
    }
    catch {
      case e: IOException => {
        throw new RuntimeException("Error serializing", e)
      }
    }
  }

  //precompute schemas
  //TODO make this class abstract and extract schemas to factories
  private val _recordSchema: Schema = __schemaStore.getSchema(tileDataType, getAvroType(tileDataType).get)
  private val _sparseSchema: Schema = new AvroSchemaComposer().add(_recordSchema).addResource("sparseTile.avsc").resolved
  private val _denseSchema: Schema = new AvroSchemaComposer().add(_recordSchema).addResource("denseTile.avsc").resolved
  private val _sparseBinSchema: Schema = _sparseSchema.getField("values").schema.getElementType
  private val _denseBinSchema: Schema = _denseSchema.getField("values").schema.getElementType

  //pre-instantiate some records to use for serialization
  private val sparseTileRecord = new GenericData.Record(_sparseSchema)
  private val denseTileRecord = new GenericData.Record(_denseSchema)
  private val sparseBins: java.util.ArrayList[GenericRecord] = new java.util.ArrayList[GenericRecord](maxBinCount)
  private val denseBins: java.util.ArrayList[GenericRecord] = new java.util.ArrayList[GenericRecord](maxBinCount)
  private val valueRecords: java.util.ArrayList[GenericRecord] = new java.util.ArrayList[GenericRecord](maxBinCount)
  for (i <- 0 until maxBinCount) {
    sparseBins.add(i, new GenericData.Record(_sparseBinSchema))
    denseBins.add(i, new GenericData.Record(_denseBinSchema))
    valueRecords.add(i, new GenericData.Record(_recordSchema))
  }
  private val defaultValueRecord = new GenericData.Record(_recordSchema)

  private def serializeSparse(tileData: TileData[V, X], stream: ByteArrayOutputStream): Unit = {
    var i=0
    val projection = tileData.projection
    for (x <- 0 until projection.xBins) {
      for (y <- 0 until projection.yBins) {
        sparseBins.get(i).put("xIndex", x)
        sparseBins.get(i).put("yIndex", y)
        setValue(valueRecords.get(i), tileData.getBin(x, y))
        sparseBins.get(i).put("value", valueRecords.get(i))
        i+=1
      }
    }
    sparseTileRecord.put("level", tileData.z)
    sparseTileRecord.put("xIndex", tileData.x)
    sparseTileRecord.put("yIndex", tileData.y)
    sparseTileRecord.put("xBinCount", projection.xBins)
    sparseTileRecord.put("yBinCount", projection.yBins)
    sparseTileRecord.put("values", sparseBins.subList(0, projection.xBins*projection.yBins))
    sparseTileRecord.put("meta", getTileMetaData(tileData))
    setValue(defaultValueRecord, tileData.defaultBinValue)
    sparseTileRecord.put("default", defaultValueRecord)

    writeRecord(sparseTileRecord, _sparseSchema, stream)
  }

  private def serializeDense(tileData: TileData[V, X], stream: ByteArrayOutputStream): Unit = {
    var i=0
    val projection = tileData.projection
    for (x <- 0 until projection.xBins) {
      for (y <- 0 until projection.yBins) {
        setValue(valueRecords.get(i), tileData.getBin(x, y))
        i+=1
      }
    }
    denseTileRecord.put("level", tileData.z)
    denseTileRecord.put("xIndex", tileData.x)
    denseTileRecord.put("yIndex", tileData.y)
    denseTileRecord.put("xBinCount", projection.xBins)
    denseTileRecord.put("yBinCount", projection.yBins)
    denseTileRecord.put("values", valueRecords.subList(0, projection.xBins*projection.yBins))
    denseTileRecord.put("meta", getTileMetaData(tileData))
    sparseTileRecord.put("default", null)

    writeRecord(denseTileRecord, _denseSchema, stream)
  }


  override def serialize(tileData: TileData[V, X]): Array[Byte] = {
    val os = new ByteArrayOutputStream() //TODO pool?
    //decide between sparse and dense representation based on standard Tiles heuristic
    tileData.binsTouched > tileData.projection.xBins*tileData.projection.yBins/2 match {
      case false => serializeSparse(tileData, os)
      case _ => serializeDense(tileData, os)
    }
    os.toByteArray
  }
}
