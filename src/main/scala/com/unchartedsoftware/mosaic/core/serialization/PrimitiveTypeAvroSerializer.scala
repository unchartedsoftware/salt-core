package com.unchartedsoftware.mosaic.core.serialization

import java.io.{ByteArrayOutputStream, IOException, OutputStream}

import com.unchartedsoftware.mosaic.core.generation.output.TileData
import com.unchartedsoftware.mosaic.core.analytic.numeric.MaxMinAggregator
import com.unchartedsoftware.mosaic.core.projection._
import org.apache.avro.Schema
import org.apache.avro.file.{CodecFactory, DataFileWriter}
import org.apache.avro.generic.{GenericData, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.DatumWriter
import scala.collection.JavaConversions._

/**
 * Suitable for serializing tiles where values are primitive values.
 * Currently supports SeriesProjection and CartesianProjection
 * @tparam TC the abstract type representing a tile coordinate. Must feature a zero-arg constructor.
 * @tparam V Output data type for bin aggregators, and input for tile aggregator
 * @tparam X Output data type for tile aggregators
 */
class PrimitiveTypeAvroSerializer[TC <:TileCoord, V, X](val tileDataType: Class[_ <: V], val maxBinCount: Int) extends Serializer[TC, V, X] {

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

  //Determines if a class represents a valid Avro primitive type
  private def isValidPrimitive(candidateType: Class[_ <: V]): Boolean = {
    VALID_PRIMITIVE_TYPES.contains(candidateType)
  }

  //Get the avro string type of a valid primitive type
  def getAvroType(key: Class[_ <: V]): Option[String] = {
    VALID_PRIMITIVE_TYPES.get(key)
  }

  //TODO make this abstract
  def setValue(valueRecord: GenericRecord, value: V) = {
    valueRecord.put("value", value)
  }

  //FIXME This assumes that tile aggregator is always MaxMinAggregator...
  def getTileMetaData(tileData: TileData[TC, V, X]): java.util.Map[String, String] = {
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
  private val _sparseSchema: Schema = new AvroSchemaComposer().add(_recordSchema).addResource("sparseTileSchema.avsc").resolved
  private val _denseSchema: Schema = new AvroSchemaComposer().add(_recordSchema).addResource("denseTileSchema.avsc").resolved
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

  private def serializeSparse(tileData: TileData[TC, V, X], stream: ByteArrayOutputStream): Unit = {
    //TODO eliminate duplicate code between this and serializeDense
    var i=0
    val projection = tileData.projection
    val xBins = projection match {
      case s: SeriesProjection => s.bins
      case p: CartesianProjection => p.xBins
    }
    val yBins = projection match {
      case s: SeriesProjection => 1
      case p: CartesianProjection => p.yBins
    }
    for (x <- 0 until xBins) {
      for (y <- 0 until yBins) {
        val value = tileData.getBin(x + y*xBins)
        if (!value.equals(tileData.defaultBinValue)) {
          val valueRecord = valueRecords.get(i)
          sparseBins.get(i).put("xIndex", x)
          sparseBins.get(i).put("yIndex", y)
          setValue(valueRecord, value)
          sparseBins.get(i).put("value", valueRecord)
          i+=1
        }
      }
    }
    tileData.coords match {
      case s: SeriesCoord => {
        sparseTileRecord.put("level", s.z)
        sparseTileRecord.put("xIndex", s.x)
        sparseTileRecord.put("yIndex", 1)
      }
      case p: CartesianCoord => {
        sparseTileRecord.put("level", p.z)
        sparseTileRecord.put("xIndex", p.x)
        sparseTileRecord.put("yIndex", p.y)
      }
    }
    tileData.projection match {
      case s: SeriesProjection => {
        sparseTileRecord.put("xBinCount", s.bins)
        sparseTileRecord.put("yBinCount", 1)
      }
      case p: CartesianProjection => {
        sparseTileRecord.put("xBinCount", p.xBins)
        sparseTileRecord.put("yBinCount", p.yBins)
      }
    }
    sparseTileRecord.put("values", sparseBins.subList(0, i))
    sparseTileRecord.put("meta", getTileMetaData(tileData))
    setValue(defaultValueRecord, tileData.defaultBinValue)
    sparseTileRecord.put("default", defaultValueRecord)

    writeRecord(sparseTileRecord, _sparseSchema, stream)
  }

  private def serializeDense(tileData: TileData[TC, V, X], stream: ByteArrayOutputStream): Unit = {
    //TODO eliminate duplicate code between this and serializeSparse
    var i=0
    val projection = tileData.projection
    val xBins = tileData.projection match {
      case s: SeriesProjection => s.bins
      case p: CartesianProjection => p.xBins
    }
    val yBins = tileData.projection match {
      case s: SeriesProjection => 1
      case p: CartesianProjection => p.yBins
    }
    for (y <- 0 until xBins) {
      for (x <- 0 until yBins) {
        setValue(valueRecords.get(i), tileData.getBin(x + y*xBins))
        i+=1
      }
    }
    tileData.coords match {
      case s: SeriesCoord => {
        denseTileRecord.put("level", s.z)
        denseTileRecord.put("xIndex", s.x)
        denseTileRecord.put("yIndex", 1)
      }
      case p: CartesianCoord => {
        denseTileRecord.put("level", p.z)
        denseTileRecord.put("xIndex", p.x)
        denseTileRecord.put("yIndex", p.y)
      }
    }
    tileData.projection match {
      case s: SeriesProjection => {
        denseTileRecord.put("xBinCount", s.bins)
        denseTileRecord.put("yBinCount", 1)
      }
      case p: CartesianProjection => {
        denseTileRecord.put("xBinCount", p.xBins)
        denseTileRecord.put("yBinCount", p.yBins)
      }
    }
    denseTileRecord.put("values", valueRecords.subList(0, projection.bins))
    denseTileRecord.put("meta", getTileMetaData(tileData))
    denseTileRecord.put("default", null)

    writeRecord(denseTileRecord, _denseSchema, stream)
  }


  override def serialize(tileData: TileData[TC, V, X]): Array[Byte] = {
    val os = new ByteArrayOutputStream() //TODO pool?
    //decide between sparse and dense representation based on standard Tiles heuristic
    tileData.binsTouched > tileData.projection.bins/2 match {
      case false => serializeSparse(tileData, os)
      case _ => serializeDense(tileData, os)
    }
    os.toByteArray
  }
}
