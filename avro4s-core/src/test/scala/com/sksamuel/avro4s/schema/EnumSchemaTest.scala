package com.sksamuel.avro4s.schema

import com.sksamuel.avro4s.{AvroEnumDefault, AvroName, AvroNamespace, AvroProp, AvroSchema, AvroSortPriority, SchemaFor}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EnumSchemaTest extends AnyWordSpec with Matchers {

  //  implicit val schemaForWine: SchemaFor[Wine] = JavaEnumSchemaFor[Wine](default = Wine.Shiraz)
  //  implicit val schemaForColor: SchemaFor[Colours.Value] = ScalaEnumSchemaFor[Colours.Value](default = Colours.Amber)

  "SchemaEncoder" should {

    //------------------------------------------------------------------------------------------------------------------
    // java enums using the AvroJavaEnumDefault annotation

    "support top level java enums using the AvroJavaName, AvroJavaNamespace, AvroJavaProp, AvroJavaEnumDefault annotations" in {

      val schema = AvroSchema[WineWithAnnotations]
      val expected = new org.apache.avro.Schema.Parser().parse(getClass.getResourceAsStream("/java_enum_top_level_with_default.json"))
      schema.toString(true) shouldBe expected.toString(true)
    }

    //------------------
    // java enums

    "support top level java enums" in {
      val schema = AvroSchema[Wine]
      val expected = new org.apache.avro.Schema.Parser().parse(getClass.getResourceAsStream("/java_enum_top_level.json"))
      schema.toString(true) shouldBe expected.toString(true)
    }

    // todo tests for java enums are broken by magnolia 1.3.3
    // "support java enums" in {
    //   case class JavaEnum(wine: Wine)
    //   val schema = AvroSchema[JavaEnum]
    //   val expected = new org.apache.avro.Schema.Parser().parse(getClass.getResourceAsStream("/java_enum.json"))
    //   schema.toString(true) shouldBe expected.toString(true)
    // }

    // todo tests for java enums are broken by magnolia 1.3.3
      //  "support java enums with default values" in {
    
      //    case class JavaEnumWithDefaultValue(wine: Wine = Wine.CabSav)
    
      //    val schema = AvroSchema[JavaEnumWithDefaultValue]
      //    val expected = new org.apache.avro.Schema.Parser().parse(
      //      """
      //        |{
      //        |  "type" : "record",
      //        |  "name" : "JavaEnumWithDefaultValue",
      //        |  "namespace" : "com.sksamuel.avro4s.schema.EnumSchemaTest",
      //        |  "fields" : [ {
      //        |    "name" : "wine",
      //        |    "type" : {
      //        |      "type" : "enum",
      //        |      "name" : "Wine",
      //        |      "namespace" : "com.sksamuel.avro4s.schema",
      //        |      "symbols" : [ "Malbec", "Shiraz", "CabSav", "Merlot" ],
      //        |      "default" : "Shiraz"
      //        |    },
      //        |    "default" : "CabSav"
      //        |  } ]
      //        |}
      //        |""".stripMargin
      //    )
    
      //    schema.toString(true) shouldBe expected.toString(true)
      //  }

    // todo tests for java enums are broken by magnolia 1.3.3
    // "support optional java enums" in {

    //   case class OptionalJavaEnum(wine: Option[Wine])

    //   val schema = AvroSchema[OptionalJavaEnum]
    //   val expected = new org.apache.avro.Schema.Parser().parse(getClass.getResourceAsStream("/java_enum_option.json"))

    //   schema.toString(true) shouldBe expected.toString(true)
    // }

    // todo tests for java enums are broken by magnolia 1.3.3
    //    "support optional java enums with default none" in {
    //
    //      case class OptionalJavaEnumWithDefaultNone(wine: Option[Wine] = None)
    //
    //      val schema = AvroSchema[OptionalJavaEnumWithDefaultNone]
    //      val expected = new org.apache.avro.Schema.Parser().parse(
    //        """
    //          |{
    //          |  "type": "record",
    //          |  "name": "OptionalJavaEnumWithDefaultNone",
    //          |  "namespace": "com.sksamuel.avro4s.schema.EnumSchemaTest",
    //          |  "fields": [
    //          |    {
    //          |      "name": "wine",
    //          |      "type": [
    //          |        "null",
    //          |        {
    //          |          "type": "enum",
    //          |          "name": "Wine",
    //          |          "namespace": "com.sksamuel.avro4s.schema",
    //          |          "symbols": [
    //          |            "Malbec",
    //          |            "Shiraz",
    //          |            "CabSav",
    //          |            "Merlot"
    //          |          ],
    //          |          "default": "Shiraz"
    //          |        }
    //          |      ],
    //          |      "default": null
    //          |    }
    //          |  ]
    //          |}
    //          |""".stripMargin
    //      )
    //
    //      schema.toString(true) shouldBe expected.toString(true)
    //    }

    // todo tests for java enums are broken by magnolia 1.3.3
    //    "support optional java enums with default values" in {
    //
    //      case class OptionalJavaEnumWithDefaultValue(wine: Option[Wine] = Some(Wine.CabSav))
    //
    //      val schema = AvroSchema[OptionalJavaEnumWithDefaultValue]
    //      val expected = new org.apache.avro.Schema.Parser().parse(
    //        """
    //          |{
    //          |  "type": "record",
    //          |  "name": "OptionalJavaEnumWithDefaultValue",
    //          |  "namespace": "com.sksamuel.avro4s.schema.EnumSchemaTest",
    //          |  "fields": [
    //          |    {
    //          |      "name": "wine",
    //          |      "type": [
    //          |        {
    //          |          "type": "enum",
    //          |          "name": "Wine",
    //          |          "namespace": "com.sksamuel.avro4s.schema",
    //          |          "symbols": [
    //          |            "Malbec",
    //          |            "Shiraz",
    //          |            "CabSav",
    //          |            "Merlot"
    //          |          ],
    //          |          "default": "Shiraz"
    //          |        },
    //          |        "null"
    //          |      ],
    //          |      "default": "CabSav"
    //          |    }
    //          |  ]
    //          |}
    //          |""".stripMargin
    //      )
    //
    //      schema.toString(true) shouldBe expected.toString(true)
    //    }

    //----------------------------------------
    // scala enums using ScalaEnumSchemaFor

       "support top level scala enums with symbols sorted alphabetically by default (because subtypes are always sorted by magnolia1)" in {
    
         val schema = AvroSchema[Colours]
         val expected = new org.apache.avro.Schema.Parser().parse(
           """
             |{
             |  "type": "enum",
             |  "name": "Colours",
             |  "namespace": "com.sksamuel.avro4s.schema",
             |  "symbols": [
             |    "Amber",
             |    "Green",
             |    "Red"
             |  ]
             |}
             |""".stripMargin.trim
         )
    
         schema.toString(true) shouldBe expected.toString(true)
       }
    
       "support scala enums" in {
    
         case class ScalaEnum(colours: Colours)
    
         val schema = AvroSchema[ScalaEnum]
         val expected = new org.apache.avro.Schema.Parser().parse(
           """
             |{
             |  "type" : "record",
             |  "name" : "ScalaEnum",
             |  "namespace" : "com.sksamuel.avro4s.schema.EnumSchemaTest",
             |  "fields" : [ {
             |    "name" : "colours",
             |    "type" : {
             |      "type" : "enum",
             |      "name" : "Colours",
             |      "namespace" : "com.sksamuel.avro4s.schema",
             |      "symbols" : [ "Amber", "Green", "Red" ]
             |    }
             |  } ]
             |}
             |""".stripMargin.trim
         )
    
         schema.toString(true) shouldBe expected.toString(true)
       }

       "support scala enums with default values" in {
    
         case class ScalaEnumWithDefaultValue(colours: Colours = Colours.Red)
    
         val schema = AvroSchema[ScalaEnumWithDefaultValue]
         val expected = new org.apache.avro.Schema.Parser().parse(
           """
             |{
             |  "type": "record",
             |  "name": "ScalaEnumWithDefaultValue",
             |  "namespace": "com.sksamuel.avro4s.schema.EnumSchemaTest",
             |  "fields": [
             |    {
             |      "name": "colours",
             |      "type": {
             |        "type": "enum",
             |        "name": "Colours",
             |        "namespace": "com.sksamuel.avro4s.schema",
             |        "symbols": [
             |          "Amber",
             |          "Green",
             |          "Red"
             |        ]
             |      },
             |      "default": "Red"
             |    }
             |  ]
             |}
             |""".stripMargin
         )
    
         schema.toString(true) shouldBe expected.toString(true)
       }
    
       "support optional scala enums" in {
    
         case class OptionalScalaEnum(color: Option[Colours])
    
         val schema = AvroSchema[OptionalScalaEnum]
         val expected = new org.apache.avro.Schema.Parser().parse(
           """
             |{
             |  "type": "record",
             |  "name": "OptionalScalaEnum",
             |  "namespace": "com.sksamuel.avro4s.schema.EnumSchemaTest",
             |  "fields": [
             |    {
             |      "name": "color",
             |      "type": [
             |        "null",
             |        {
             |          "type": "enum",
             |          "namespace": "com.sksamuel.avro4s.schema",
             |          "name": "Colours",
             |          "symbols": [
             |            "Amber",
             |            "Green",
             |            "Red"
             |          ]
             |        }
             |      ]
             |    }
             |  ]
             |}
             |""".stripMargin
         )
    
         schema.toString(true) shouldBe expected.toString(true)
       }

       "support optional scala enums with default none" in {
    
         case class OptionalScalaEnumWithDefaultNone(color: Option[Colours] = None)
    
         val schema = AvroSchema[OptionalScalaEnumWithDefaultNone]

         val expected = new org.apache.avro.Schema.Parser().parse(
           """
             |{
             |  "type": "record",
             |  "name": "OptionalScalaEnumWithDefaultNone",
             |  "namespace": "com.sksamuel.avro4s.schema.EnumSchemaTest",
             |  "fields": [
             |    {
             |      "name": "color",
             |      "type": [
             |        "null",
             |        {
             |          "type": "enum",
             |          "namespace": "com.sksamuel.avro4s.schema",
             |          "name": "Colours",
             |          "symbols": [
             |            "Amber",
             |            "Green",
             |            "Red"
             |          ]
             |        }
             |      ],
             |      "default": null
             |    }
             |  ]
             |}
             |""".stripMargin
         )
    
         schema.toString(true) shouldBe expected.toString(true)
       }

       "support optional scala enums with a default value" in {
    
         case class OptionalScalaEnumWithDefaultValue(coloursopt: Option[Colours] = Some(Colours.Red))
    
         val schema = AvroSchema[OptionalScalaEnumWithDefaultValue]

         val expected = new org.apache.avro.Schema.Parser().parse(
           """
             |{
             |  "type": "record",
             |  "name": "OptionalScalaEnumWithDefaultValue",
             |  "namespace": "com.sksamuel.avro4s.schema.EnumSchemaTest",
             |  "fields": [
             |    {
             |      "name": "coloursopt",
             |      "type": [
             |        {
             |          "type": "enum",
             |          "namespace": "com.sksamuel.avro4s.schema",
             |          "name": "Colours",
             |          "symbols": [
             |            "Amber",
             |            "Green",
             |            "Red"
             |          ]
             |        },
             |        "null"
             |      ],
             |      "default": "Red"
             |    }
             |  ]
             |}
             |""".stripMargin
         )
    
         schema.toString(true) shouldBe expected.toString(true)
       }

       //------------------------------------------------------------------------------------------------------------------
       // scala enums using the AvroEnumDefault annotation
    
       "support top level scala enums using the AvroEnumDefault annotation" in {
    
         val schema = AvroSchema[ColoursAnnotatedEnum]

         val expected = new org.apache.avro.Schema.Parser().parse(
           """
             |{
             |  "type": "enum",
             |  "name": "Colours",
             |  "namespace": "test",
             |  "symbols": [
             |    "Red",
             |    "Amber",
             |    "Green"
             |  ],
             |  "default": "Green",
             |  "hello": "world"
             |}
             |""".stripMargin.trim
         )
    
         schema.toString(true) shouldBe expected.toString(true)
       }
    
       //------------------------------------------------------------------------------------------------------------------
       // sealed trait enums
    
       "support top level sealed trait enums with no default enum value" in {
         val schema = AvroSchema[CupcatEnum]
         val expected = new org.apache.avro.Schema.Parser().parse(
           """
             |{
             |  "type" : "enum",
             |  "name" : "CupcatEnum",
             |  "namespace" : "com.sksamuel.avro4s.schema",
             |  "symbols" : [  "SnoutleyEnum", "CuppersEnum" ]
             |}
             |""".stripMargin
         )
    
         schema.toString(true) shouldBe expected.toString(true)
       }

    "support sealed trait enums with no default enum value" in {
      case class SealedTraitEnum(cupcat: CupcatEnum)
      val schema = AvroSchema[SealedTraitEnum]
      val expected = new org.apache.avro.Schema.Parser().parse(getClass.getResourceAsStream("/sealed_trait_enum_no_default.json"))
      schema.toString(true) shouldBe expected.toString(true)
    }

    "support sealed trait enums with no default enum value and with a default field value" in {

      case class SealedTraitEnumWithDefaultValue(cupcat: CupcatEnum = CuppersEnum)

      val schema = AvroSchema[SealedTraitEnumWithDefaultValue]
      val expected = new org.apache.avro.Schema.Parser().parse(
        """
          |{
          |  "type" : "record",
          |  "name" : "SealedTraitEnumWithDefaultValue",
          |  "namespace" : "com.sksamuel.avro4s.schema.EnumSchemaTest",
          |  "fields" : [ {
          |    "name" : "cupcat",
          |    "type" : {
          |      "type" : "enum",
          |      "name" : "CupcatEnum",
          |      "namespace" : "com.sksamuel.avro4s.schema",
          |      "symbols" : [ "SnoutleyEnum", "CuppersEnum" ]
          |    },
          |    "default" : "CuppersEnum"
          |  } ]
          |}
          |""".stripMargin
      )

      schema.toString(true) shouldBe expected.toString(true)
    }
    
    "support optional sealed trait enums with no default enum value" in {
      case class OptionalSealedTraitEnum(cupcat: Option[CupcatEnum])
      val schema = AvroSchema[OptionalSealedTraitEnum]
      val expected = new org.apache.avro.Schema.Parser().parse(getClass.getResourceAsStream("/optional_sealed_trait_enum.json"))
      schema.toString(true) shouldBe expected.toString(true)
    }

    "support optional sealed trait enums with no default enum value but with a default field value of none" in {

      case class OptionalSealedTraitEnumWithDefaultNone(cupcat: Option[CupcatEnum] = None)

      val schema = AvroSchema[OptionalSealedTraitEnumWithDefaultNone]

      val expected = new org.apache.avro.Schema.Parser().parse(
        """
          |{
          |  "type" : "record",
          |  "name" : "OptionalSealedTraitEnumWithDefaultNone",
          |  "namespace" : "com.sksamuel.avro4s.schema.EnumSchemaTest",
          |  "fields" : [ {
          |    "name" : "cupcat",
          |    "type" : [ "null", {
          |      "type" : "enum",
          |      "name" : "CupcatEnum",
          |      "namespace" : "com.sksamuel.avro4s.schema",
          |      "symbols" : [ "SnoutleyEnum", "CuppersEnum" ]
          |    } ],
          |    "default" : null
          |  } ]
          |}
          |""".stripMargin
      )

      schema.toString(true) shouldBe expected.toString(true)
    }

    // todo magnolia doesn't yet support defaults
       "support optional sealed trait enums with no default enum value but with a default field value" in {
    
         case class OptionalSealedTraitEnumWithDefaultValue(cupcat: Option[CupcatEnum] = Option(SnoutleyEnum))
    
         val schema = AvroSchema[OptionalSealedTraitEnumWithDefaultValue]
    
         val expected = new org.apache.avro.Schema.Parser().parse(
           """
             |{
             |  "type" : "record",
             |  "name" : "OptionalSealedTraitEnumWithDefaultValue",
             |  "namespace" : "com.sksamuel.avro4s.schema.EnumSchemaTest",
             |  "fields" : [ {
             |    "name" : "cupcat",
             |    "type" : [ {
             |      "type" : "enum",
             |      "name" : "CupcatEnum",
             |      "namespace" : "com.sksamuel.avro4s.schema",
             |      "symbols" : [ "SnoutleyEnum", "CuppersEnum" ]
             |    }, "null" ],
             |    "default": "SnoutleyEnum"
             |  } ]
             |}
             |""".stripMargin
         )
    
         schema.toString(true) shouldBe expected.toString(true)
       }
    
       //------------------
       // sealed trait enums with annotation default

       "support sealed trait enums with a default enum value and no default field value" in {
    
         case class AnnotatedSealedTraitEnum(cupcat: CupcatAnnotatedEnum)
    
         val schema = AvroSchema[AnnotatedSealedTraitEnum]
         val expected = new org.apache.avro.Schema.Parser().parse(
           """
             |{
             |  "type" : "record",
             |  "name" : "AnnotatedSealedTraitEnum",
             |  "namespace" : "com.sksamuel.avro4s.schema.EnumSchemaTest",
             |  "fields" : [ {
             |    "name" : "cupcat",
             |    "type" : {
             |      "type" : "enum",
             |      "name" : "CupcatAnnotatedEnum",
             |      "namespace" : "com.sksamuel.avro4s.schema",
             |      "symbols" : [ "CuppersAnnotatedEnum", "SnoutleyAnnotatedEnum" ],
             |      "default" : "SnoutleyAnnotatedEnum"
             |    }
             |  } ]
             |}
             |""".stripMargin
         )
    
         schema.toString(true) shouldBe expected.toString(true)
       }
    
       "support sealed trait enums with a default enum value and a default field value" in {
    
         case class AnnotatedSealedTraitEnumWithDefaultValue(cupcat: CupcatAnnotatedEnum = CuppersAnnotatedEnum)
    
         val schema = AvroSchema[AnnotatedSealedTraitEnumWithDefaultValue]
         val expected = new org.apache.avro.Schema.Parser().parse(
           """
             |{
             |  "type" : "record",
             |  "name" : "AnnotatedSealedTraitEnumWithDefaultValue",
             |  "namespace" : "com.sksamuel.avro4s.schema.EnumSchemaTest",
             |  "fields" : [ {
             |    "name" : "cupcat",
             |    "type" : {
             |      "type" : "enum",
             |      "name" : "CupcatAnnotatedEnum",
             |      "namespace" : "com.sksamuel.avro4s.schema",
             |      "symbols" : [ "CuppersAnnotatedEnum", "SnoutleyAnnotatedEnum" ],
             |      "default" : "SnoutleyAnnotatedEnum"
             |    },
             |    "default" : "CuppersAnnotatedEnum"
             |  } ]
             |}
             |""".stripMargin
         )
    
         schema.toString(true) shouldBe expected.toString(true)
       }

       "support optional sealed trait enums with a default enum value and no default field value" in {
    
         case class OptionalAnnotatedSealedTraitEnum(cupcat: Option[CupcatAnnotatedEnum])
    
         val schema = AvroSchema[OptionalAnnotatedSealedTraitEnum]
         val expected = new org.apache.avro.Schema.Parser().parse(
           """
             |{
             |  "type" : "record",
             |  "name" : "OptionalAnnotatedSealedTraitEnum",
             |  "namespace" : "com.sksamuel.avro4s.schema.EnumSchemaTest",
             |  "fields" : [ {
             |    "name" : "cupcat",
             |    "type" : [ "null", {
             |      "type" : "enum",
             |      "name" : "CupcatAnnotatedEnum",
             |      "namespace" : "com.sksamuel.avro4s.schema",
             |      "symbols" : [ "CuppersAnnotatedEnum", "SnoutleyAnnotatedEnum" ],
             |      "default" : "SnoutleyAnnotatedEnum"
             |    } ]
             |  } ]
             |}
             |""".stripMargin
         )
    
         schema.toString(true) shouldBe expected.toString(true)
       }
    
       "support optional sealed trait enums with a default enum value and a default field value of none" in {
    
         case class OptionalAnnotatedSealedTraitEnumWithDefaultNone(cupcat: Option[CupcatAnnotatedEnum] = None)
    
         val schema = AvroSchema[OptionalAnnotatedSealedTraitEnumWithDefaultNone]
         val expected = new org.apache.avro.Schema.Parser().parse(
           """
             |{
             |  "type" : "record",
             |  "name" : "OptionalAnnotatedSealedTraitEnumWithDefaultNone",
             |  "namespace" : "com.sksamuel.avro4s.schema.EnumSchemaTest",
             |  "fields" : [ {
             |    "name" : "cupcat",
             |    "type" : [ "null", {
             |      "type" : "enum",
             |      "name" : "CupcatAnnotatedEnum",
             |      "namespace" : "com.sksamuel.avro4s.schema",
             |      "symbols" : [ "CuppersAnnotatedEnum", "SnoutleyAnnotatedEnum" ],
             |      "default" : "SnoutleyAnnotatedEnum"
             |    } ],
             |    "default": null
             |  } ]
             |}
             |""".stripMargin
         )
    
         schema.toString(true) shouldBe expected.toString(true)
       }

       "support optional sealed trait enums with a default enum value and a default field value" in {
    
         case class OptionalAnnotatedSealedTraitEnumWithDefaultValue(cupcat: Option[CupcatAnnotatedEnum] = Option(CuppersAnnotatedEnum))
    
         val schema = AvroSchema[OptionalAnnotatedSealedTraitEnumWithDefaultValue]
         val expected = new org.apache.avro.Schema.Parser().parse(
           """
             |{
             |  "type" : "record",
             |  "name" : "OptionalAnnotatedSealedTraitEnumWithDefaultValue",
             |  "namespace" : "com.sksamuel.avro4s.schema.EnumSchemaTest",
             |  "fields" : [ {
             |    "name" : "cupcat",
             |    "type" : [ {
             |      "type" : "enum",
             |      "name" : "CupcatAnnotatedEnum",
             |      "namespace" : "com.sksamuel.avro4s.schema",
             |      "symbols" : [ "CuppersAnnotatedEnum", "SnoutleyAnnotatedEnum" ],
             |      "default" : "SnoutleyAnnotatedEnum"
             |    },
             |    "null" ],
             |    "default": "CuppersAnnotatedEnum"
             |  } ]
             |}
             |""".stripMargin
         )
    
         schema.toString(true) shouldBe expected.toString(true)
       }
  }
}

enum Colours:
  case Red 
  case Amber
  case Green

@AvroName("Colours")
@AvroNamespace("test")
@AvroEnumDefault(ColoursAnnotatedEnum.Green)
@AvroProp("hello", "world")
enum ColoursAnnotatedEnum:
  @AvroSortPriority(0)
  case Red
  @AvroSortPriority(1)
  case Amber
  @AvroSortPriority(2)
  case Green


sealed trait CupcatEnum
@AvroSortPriority(0) case object SnoutleyEnum extends CupcatEnum
@AvroSortPriority(1) case object CuppersEnum extends CupcatEnum

@AvroEnumDefault(SnoutleyAnnotatedEnum)
sealed trait CupcatAnnotatedEnum
@AvroSortPriority(1) case object SnoutleyAnnotatedEnum extends CupcatAnnotatedEnum
@AvroSortPriority(0) case object CuppersAnnotatedEnum extends CupcatAnnotatedEnum

