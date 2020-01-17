package com.sksamuel.avro4s.record.encoder

import com.sksamuel.avro4s.{AvroSchema, DefaultFieldMapper, Encoder, ImmutableRecord}
import org.apache.avro.util.Utf8
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class OptionEncoderTest extends AnyWordSpec with Matchers {

  "Encoder" should {
    "support String options" in {
      case class Test(s: Option[String])
      val schema = AvroSchema[Test]
      Encoder[Test].encode(Test(Option("qwe")), schema, DefaultFieldMapper) shouldBe ImmutableRecord(schema, Vector(new Utf8("qwe")))
      Encoder[Test].encode(Test(None), schema, DefaultFieldMapper) shouldBe ImmutableRecord(schema, Vector(null))
    }
    "support boolean options" in {
      case class Test(b: Option[Boolean])
      val schema = AvroSchema[Test]
      Encoder[Test].encode(Test(Option(true)), schema, DefaultFieldMapper) shouldBe ImmutableRecord(schema, Vector(java.lang.Boolean.valueOf(true)))
      Encoder[Test].encode(Test(None), schema, DefaultFieldMapper) shouldBe ImmutableRecord(schema, Vector(null))
    }
    "support options of case classes" in {
      case class Foo(s: String)
      case class Test(b: Option[Foo])
      val schema = AvroSchema[Test]
      val fooSchema = AvroSchema[Foo]
      Encoder[Test].encode(Test(Option(Foo("hello"))), schema, DefaultFieldMapper) shouldBe ImmutableRecord(schema, Vector(ImmutableRecord(fooSchema, Vector(new Utf8("hello")))))
      Encoder[Test].encode(Test(None), schema, DefaultFieldMapper) shouldBe ImmutableRecord(schema, Vector(null))
    }
  }
}

