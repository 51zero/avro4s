package com.sksamuel.avro4s

import java.time.Instant

import magnolia1.SealedTrait
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization.write
import org.apache.avro.Schema
import org.apache.avro.Schema.Type
import org.json4s.DefaultFormats
import org.json4s.jvalue2extractable
import scala.reflect.Enum
import scala.collection.JavaConverters._
import com.sksamuel.avro4s.typeutils.Annotations

sealed trait CustomDefault
case class CustomUnionDefault(className: String, values: java.util.Map[String, Any]) extends CustomDefault
case class CustomUnionWithEnumDefault(parentName: String, default: String, value: String) extends CustomDefault
case class CustomEnumDefault(value: String) extends CustomDefault

object CustomDefaults {

 given formats: DefaultFormats = DefaultFormats

 def customScalaEnumDefault(value: Any) = CustomEnumDefault(value.toString)

 def customDefault(p: Product, schema: Schema): CustomDefault =
   if(isEnum(p, schema.getType))
    //  CustomEnumDefault(trimmedClassName(p))
     CustomEnumDefault(p.toString())
   else {
     if(isUnionOfEnum(schema)) {
       val enumType = schema.getTypes.asScala.filter(_.getType == Schema.Type.ENUM).head
       CustomUnionWithEnumDefault(enumType.getName, trimmedClassName(p), p.toString)
     } else
       CustomUnionDefault(trimmedClassName(p), parse(write(p)).extract[Map[String, Any]].map {
         case (name, b: BigInt) if b.isValidInt => name -> b.intValue
         case (name, b: BigInt) if b.isValidLong => name -> b.longValue
         case (name, z) if schema.getType == Type.UNION => name ->
           schema.getTypes.asScala.find(_.getName == trimmedClassName(p)).map(_.getField(name).schema())
             .map(DefaultResolver(z, _)).getOrElse(z)
         case (name, z) => name -> DefaultResolver(z, schema.getField(name).schema())

       }.asJava)
   }

 def isUnionOfEnum(schema: Schema) = 
    val types = schema.getTypes.asScala.map(_.getType)
    schema.getType == Schema.Type.UNION && schema.getTypes.asScala.map(_.getType).contains(Schema.Type.ENUM)

 def sealedTraitEnumDefaultValue[T](ctx: SealedTrait[SchemaFor, T]): Option[String] =
    val defaultExtractor = Annotations(ctx.annotations)
    defaultExtractor.enumDefault.flatMap { default =>
        ctx.subtypes.flatMap { (st: SealedTrait.Subtype[SchemaFor, T, _]) =>
        if st.typeInfo.short == default.toString then
            Some(st.typeInfo.short)
        else
            None
        }.headOption
    }

 def isScalaEnumeration(value: Any) = 
    value.isInstanceOf[Enum]

 def customInstantDefault(instant: Instant): java.lang.Long = instant match {
   case Instant.MAX => Instant.ofEpochMilli(Long.MaxValue).toEpochMilli()
   case Instant.MIN => Instant.ofEpochMilli(Long.MinValue).toEpochMilli()
   case _ => instant.toEpochMilli()
 }

 private def isEnum(product: Product, schemaType: Schema.Type) =
   product.productArity == 0 && schemaType == Schema.Type.ENUM

 private def trimmedClassName(p: Product) = trimDollar(p.getClass.getSimpleName)

 private def trimDollar(s: String) = if(s.endsWith("$")) s.dropRight(1) else s
}
