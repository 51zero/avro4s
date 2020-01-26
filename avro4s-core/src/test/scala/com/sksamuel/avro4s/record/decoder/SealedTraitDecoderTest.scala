package com.sksamuel.avro4s.record.decoder

import com.sksamuel.avro4s._
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.util.Utf8
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SealedTraitDecoderTest extends AnyFunSuite with Matchers {

  test("support sealed traits of case classes") {

    val record = new GenericData.Record(AvroSchemaV2[Wrapper])
    val wobble = new GenericData.Record(AvroSchemaV2[Wobble])
    wobble.put("str", new Utf8("foo"))
    record.put("wibble", wobble)

    val wrapper = DecoderV2[Wrapper].decode(record)
    wrapper shouldBe Wrapper(Wobble("foo"))
  }

  test("support trait subtypes fields with same name") {

    val record = new GenericData.Record(AvroSchemaV2[Trapper])
    val tobble = new GenericData.Record(AvroSchemaV2[Tobble])
    tobble.put("str", new Utf8("foo"))
    tobble.put("place", new Utf8("bar"))
    record.put("tibble", tobble)

    val trapper = DecoderV2[Trapper].decode(record)
    trapper shouldBe Trapper(Tobble("foo", "bar"))
  }

  test("support trait subtypes fields with same name and same type") {

    val record = new GenericData.Record(AvroSchemaV2[Napper])
    val nabble = new GenericData.Record(AvroSchemaV2[Nabble])
    nabble.put("str", new Utf8("foo"))
    nabble.put("age", java.lang.Integer.valueOf(44))
    record.put("nibble", nabble)

    val napper = DecoderV2[Napper].decode(record)
    napper shouldBe Napper(Nabble("foo", 44))
  }

  test("support top level ADTs") {

    val nabble = new GenericData.Record(AvroSchemaV2[Nabble])
    nabble.put("str", new Utf8("foo"))
    nabble.put("age", java.lang.Integer.valueOf(44))

    DecoderV2[Nibble].decode(nabble) shouldBe Nabble("foo", 44)
  }

  test("use @AvroNamespace when choosing which type to decode") {

    val appleschema = SchemaBuilder.record("Apple").namespace("market").fields().requiredDouble("weight").endRecord()
    val orangeschema = SchemaBuilder.record("Orange").namespace("market").fields().requiredString("color").endRecord()
    val union = SchemaBuilder.unionOf().`type`(appleschema).and().`type`(orangeschema).endUnion()
    val schema = SchemaBuilder.record("Buy").fields().name("fruit").`type`(union).noDefault().endRecord()

    DecoderV2[Buy].decode(ImmutableRecord(schema, Vector(ImmutableRecord(appleschema, Vector(java.lang.Double.valueOf(0.3)))))) shouldBe Buy(Apple(0.3))
    DecoderV2[Buy].decode(ImmutableRecord(schema, Vector(ImmutableRecord(orangeschema, Vector(new Utf8("bright orange")))))) shouldBe Buy(Orange("bright orange"))
  }

  test("use @AvroNamespace and @AvroName with sealed traits of case objects") {
    val thingySchema = SchemaBuilder.enumeration("thingy").symbols("whim_wham", "widget")
    val schema = SchemaBuilder.record("ThingHolder").fields().name("thing").`type`(thingySchema).noDefault().endRecord()
    val record1 = new GenericData.Record(schema)
    record1.put("thing", "whim_wham")

    val record2 = new GenericData.Record(schema)
    record2.put("thing", "widget")

    DecoderV2[ThingHolder].decode(record1) shouldBe ThingHolder(WhimWham)
    DecoderV2[ThingHolder].decode(record2) shouldBe ThingHolder(Widget)
  }

  test("use @AvroNamespace and @AvroName with sealed traits of case objects in a round trip") {
    val thingySchema = SchemaBuilder.enumeration("thingy").symbols("whim_wham", "widget")
    val schema = SchemaBuilder.record("ThingHolder").fields().name("thing").`type`(thingySchema).noDefault().endRecord()

    val value = ThingHolder(WhimWham)
    val encodedRecord: GenericRecord = EncoderV2[ThingHolder].encode(value).asInstanceOf[GenericRecord]
    val decoded = DecoderV2[ThingHolder].decode(encodedRecord)
    decoded shouldBe value
  }

}

sealed trait Wibble
case class Wobble(str: String) extends Wibble
case class Wabble(dbl: Double) extends Wibble
case class Wrapper(wibble: Wibble)

sealed trait Tibble
case class Tobble(str: String, place: String) extends Tibble
case class Tabble(str: Double, age: Int) extends Tibble
case class Trapper(tibble: Tibble)

sealed trait Nibble
case class Nobble(str: String, place: String) extends Nibble
case class Nabble(str: String, age: Int) extends Nibble
case class Napper(nibble: Nibble)

sealed trait Fruit

@AvroNamespace("market")
final case class Apple(weight: Double) extends Fruit

@AvroNamespace("market")
final case class Orange(color: String) extends Fruit

@AvroNamespace("market")
final case class Buy(fruit: Fruit)

final case class ThingHolder(thing: Thingy)

@AvroName("thingy")
sealed trait Thingy
@AvroName("whim_wham")
case object WhimWham extends Thingy
@AvroName("widget")
case object Widget extends Thingy
