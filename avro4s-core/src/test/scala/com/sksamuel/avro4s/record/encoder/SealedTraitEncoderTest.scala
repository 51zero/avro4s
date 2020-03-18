package com.sksamuel.avro4s.record.encoder

import com.sksamuel.avro4s.{AvroSchema, Encoder}
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.util.Utf8
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SealedTraitEncoderTest extends AnyFunSuite with Matchers {

  test("support sealed traits of case classes") {
    val schema = AvroSchema[Wrapper]
    val record = Encoder[Wrapper].encode(Wrapper(Wobble("foo"))).asInstanceOf[GenericRecord]
    val wibble = record.get("wibble").asInstanceOf[GenericRecord]
    wibble.get("str") shouldBe new Utf8("foo")
    // the schema should be of the actual impl class
    wibble.getSchema shouldBe AvroSchema[Wobble]
  }

  test("support trait subtypes fields with same name") {
    val schema = AvroSchema[Trapper]
    val record = Encoder[Trapper].encode(Trapper(Tobble("foo", "bar"))).asInstanceOf[GenericRecord]
    val tobble = record.get("tibble").asInstanceOf[GenericRecord]
    tobble.get("str") shouldBe new Utf8("foo")
    tobble.get("place") shouldBe new Utf8("bar")
    tobble.getSchema shouldBe AvroSchema[Tobble]
  }

  test("support trait subtypes fields with same name and same type") {
    val schema = AvroSchema[Napper]
    val record = Encoder[Napper].encode(Napper(Nabble("foo", 44))).asInstanceOf[GenericRecord]
    val nobble = record.get("nibble").asInstanceOf[GenericRecord]
    nobble.get("str") shouldBe new Utf8("foo")
    nobble.get("age") shouldBe 44
    nobble.getSchema shouldBe AvroSchema[Nabble]
  }

  test("support top level ADTs") {
    val schema = AvroSchema[Nibble]
    val record = Encoder[Nibble].encode(Nabble("foo", 44)).asInstanceOf[GenericRecord]
    record.get("str") shouldBe new Utf8("foo")
    record.get("age") shouldBe 44
    record.getSchema shouldBe AvroSchema[Nabble]
  }

  test("trait of case objects should be encoded as enum") {
    val schema = AvroSchema[DibbleWrapper]
    Encoder[DibbleWrapper].encode(DibbleWrapper(Dobble)).asInstanceOf[GenericRecord].get("dibble") shouldBe new GenericData.EnumSymbol(schema, Dobble)
    Encoder[DibbleWrapper].encode(DibbleWrapper(Dabble)).asInstanceOf[GenericRecord].get("dibble") shouldBe new GenericData.EnumSymbol(schema, Dabble)
  }

  test("top level traits of case objects should be encoded as enum") {
    val schema = AvroSchema[Dibble]
    Encoder[Dibble].encode(Dobble) shouldBe new GenericData.EnumSymbol(schema, Dobble)
    Encoder[Dibble].encode(Dabble) shouldBe new GenericData.EnumSymbol(schema, Dabble)
  }

  test("options of sealed traits should be encoded correctly") {
    val schema = AvroSchema[MeasurableThing]
    val record = Encoder[MeasurableThing].encode(MeasurableThing(Some(WidthDimension(1.23)))).asInstanceOf[GenericRecord]
    val width = record.get("dimension").asInstanceOf[GenericRecord]
    width.get("width") shouldBe 1.23
  }

  test("classes nested in objects should be encoded correctly") {
    sealed trait Inner
    case class InnerOne(value: Double) extends Inner
    case class InnerTwo(height: Double) extends Inner
    case class Outer(inner: Inner)
    val schema = AvroSchema[Outer]
    val record = Encoder[Outer].encode(Outer(InnerTwo(1.23))).asInstanceOf[GenericRecord]
    val inner = record.get("inner").asInstanceOf[GenericRecord]
    inner.get("height") shouldBe 1.23
  }

  test("sealed trait classes with optional elements inside case classes should be encoded correctly") {
    case class Outer(inner: Inner)
    sealed trait Inner
    case class InnerOne(value: Double, optVal: Option[Float]) extends Inner
    case class InnerTwo(height: Double) extends Inner
    val schema = AvroSchema[Outer]
    val record = Encoder[Outer].encode(Outer(InnerOne(1.23, None))).asInstanceOf[GenericRecord]
  }
}

sealed trait Dibble
case object Dobble extends Dibble
case object Dabble extends Dibble
case class DibbleWrapper(dibble: Dibble)

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

sealed trait Dimension
case class HeightDimension(height: Double) extends Dimension
case class WidthDimension(width: Double) extends Dimension
case class MeasurableThing(dimension: Option[Dimension])

