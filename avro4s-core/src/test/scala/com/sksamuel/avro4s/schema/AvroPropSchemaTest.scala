package com.sksamuel.avro4s.schema

import com.sksamuel.avro4s.{AvroProp, AvroSchema}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AvroPropSchemaTest extends AnyWordSpec with Matchers {

  "@AvroProp" should {
    "support prop annotation on class" in {
      @AvroProp("cold", "play") case class Annotated(str: String)
      val expected = new org.apache.avro.Schema.Parser().parse(getClass.getResourceAsStream("/props_annotation_class.json"))
      val schema = AvroSchema[Annotated]
      schema.toString(true) shouldBe expected.toString(true)
    }
    "support prop annotation on field" in {
      case class Annotated(@AvroProp("cold", "play") str: String, @AvroProp("kate", "bush") long: Long, int: Int)
      val expected = new org.apache.avro.Schema.Parser().parse(getClass.getResourceAsStream("/props_annotation_field.json"))
      val schema = AvroSchema[Annotated]
      schema.toString(true) shouldBe expected.toString(true)
    }
    "support props annotations on scala enums" in {
      case class Annotated(@AvroProp("cold", "play") colours: Colours)
      val expected = new org.apache.avro.Schema.Parser().parse(getClass.getResourceAsStream("/props_annotation_scala_enum.json"))
      val schema = AvroSchema[Annotated]
      schema.toString(true) shouldBe expected.toString(true)
    }
  }
}
