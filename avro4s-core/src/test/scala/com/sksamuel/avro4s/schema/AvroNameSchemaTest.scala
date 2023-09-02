package com.sksamuel.avro4s.schema

import com.sksamuel.avro4s.{AvroName, AvroSchema, SnakeCase}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class AvroNameSchemaTest extends AnyFunSuite with Matchers {

  test("generate field names using @AvroName") {
    case class Foo(@AvroName("wibble") wobble: String, wubble: String)
    val schema = AvroSchema[Foo]
    val expected = new org.apache.avro.Schema.Parser().parse(getClass.getResourceAsStream("/avro_name_field.json"))
    schema.toString(true) shouldBe expected.toString(true)
  }

  test("generate class names using @AvroName") {
    @AvroName("wibble")
    case class Foo(a: String, b: String)
    val schema = AvroSchema[Foo]
    val expected = new org.apache.avro.Schema.Parser().parse(getClass.getResourceAsStream("/avro_name_class.json"))
    schema.toString(true) shouldBe expected.toString(true)
  }

//  test("@AvroName on top level java enum") {
//    val schema = AvroSchema[MyJavaEnum]
//    val expected = new org.apache.avro.Schema.Parser().parse(getClass.getResourceAsStream("/avro_name_java_enum.json"))
//    schema.toString(true) shouldBe expected.toString(true)
  //  }

  // todo tests for java enums are broken by magnolia 1.3.3
  // test("@AvroName on field level java enum") {
  //   case class Wibble(e: MyJavaEnum)
  //   val schema = AvroSchema[Wibble]
  //   val expected = new org.apache.avro.Schema.Parser().parse(getClass.getResourceAsStream("/avro_name_nested_java_enum.json"))
  //   schema.toString(true) shouldBe expected.toString(true)
  // }

  test("@AvroName on sealed trait enum") {
    val schema = AvroSchema[Weather]
    val expected = new org.apache.avro.Schema.Parser().parse(getClass.getResourceAsStream("/avro_name_sealed_trait.json"))
    schema.toString(true) shouldBe expected.toString(true)
  }

  test("@AvroName on sealed trait enum symbol") {
    val schema = AvroSchema[Benelux]
    val expected = new org.apache.avro.Schema.Parser().parse(getClass.getResourceAsStream("/avro_name_sealed_trait_symbol.json"))
    schema.toString(true) shouldBe expected.toString(true)
  }
}

@AvroName("foofoo")
sealed trait Weather
case object Rainy extends Weather
case object Sunny extends Weather

sealed trait Benelux
@AvroName("foofoo")
case object Belgium extends Benelux
case object Luxembourg extends Benelux
