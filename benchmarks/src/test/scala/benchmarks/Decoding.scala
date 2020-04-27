package benchmarks

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.Collections

import benchmarks.record._
import com.sksamuel.avro4s._
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import org.apache.avro.util.ByteBufferInputStream
import org.scalameter.Context
import org.scalameter.api._

object Decoding extends Bench.LocalTime with BenchmarkHelpers {
  override def defaultConfig: Context = Context(exec.minWarmupRuns -> 10000, exec.benchRuns -> 10000)

  def encode[T: Encoder: SchemaFor](value: T): ByteBuffer = {
    val outputStream = new ByteArrayOutputStream(512)
    val encoder = Encoder[T]
    val schema = AvroSchema[T]
    val record = encoder.encode(value).asInstanceOf[GenericRecord]
    val writer = new GenericDatumWriter[GenericRecord](schema)
    val enc = EncoderFactory.get().directBinaryEncoder(outputStream, null)
    writer.write(record, enc)
    ByteBuffer.wrap(outputStream.toByteArray)
  }

  def decode[T](bytes: ByteBuffer, decoder: Decoder[T], reader: GenericDatumReader[GenericRecord]): T = {
    val dec =
      DecoderFactory.get().binaryDecoder(new ByteBufferInputStream(Collections.singletonList(bytes.duplicate)), null)
    val record = reader.read(null, dec)
    decoder.decode(record)
  }

  performance of "Avro specific record union type field decoding" in {
    import benchmarks.record.generated.AttributeValue._
    import benchmarks.record.generated._
    val bytes: ByteBuffer = new RecordWithUnionAndTypeField(new ValidInt(255, t)).toByteBuffer

    using(item) in { _ =>
      RecordWithUnionAndTypeField.fromByteBuffer(bytes.duplicate)
    }
  }

  performance of "avro4s union type with type param hand-rolled decoding" in {

    import benchmarks.handrolled_codecs._
    val codec: AttributeValueCodec[Int] = AttributeValueCodec[Int]
    implicit val attributeValueEncoder = codec.encoder
    implicit val attributeValueDecoder = codec.decoder
    implicit val schemaFor: SchemaFor[AttributeValue[Int]] = SchemaFor[AttributeValue[Int]](codec.schema)
    val recordSchemaFor = SchemaFor[RecordWithUnionAndTypeField]
    val decoder = Decoder[RecordWithUnionAndTypeField].withSchema(recordSchemaFor)
    val reader = new GenericDatumReader[GenericRecord](recordSchemaFor.schema)
    val bytes = encode(RecordWithUnionAndTypeField(AttributeValue.Valid[Int](255, t)))

    using(item) in { _ =>
      decode(bytes, decoder, reader)
    }
  }

  performance of "avro4s union type with type param" in {
    implicit val mapper: FieldMapper = DefaultFieldMapper
    val decoder = Decoder[RecordWithUnionAndTypeField]
    val bytes = encode(RecordWithUnionAndTypeField(AttributeValue.Valid[Int](255, t)))
    val reader = new GenericDatumReader[GenericRecord](decoder.schema)

    using(item) in { _ =>
      val dec =
        DecoderFactory.get().binaryDecoder(new ByteBufferInputStream(Collections.singletonList(bytes.duplicate)), null)
      val record = reader.read(null, dec)
      decoder.decode(record)
    }
  }
}
