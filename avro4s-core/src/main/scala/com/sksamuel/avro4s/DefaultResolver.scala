package com.sksamuel.avro4s

import com.sksamuel.avro4s.CustomDefaults._
import org.apache.avro.LogicalTypes.Decimal
import org.apache.avro.generic.{GenericEnumSymbol, GenericFixed}
import org.apache.avro.util.Utf8
import org.apache.avro.{Conversions, Schema}

import java.lang.reflect.Field
import java.nio.ByteBuffer
import java.time.Instant
import java.util.UUID
import scala.collection.JavaConverters._
import scala.util.control.NonFatal

/**
  * When we set a default on an avro field, the type must match
  * the schema definition. For example, if our field has a schema
  * of type UUID, then the default must be a String, or for a schema
  * of Long, then the type must be a java Long and not a Scala long.
  *
  * This class will accept a scala value and convert it into a type
  * suitable for Avro and the provided schema.
  */
object DefaultResolver {

  def apply(value: Any, schema: Schema): AnyRef = value match {
    case Some(x) => apply(x, schema.getTypes.asScala.filterNot(_.getType == Schema.Type.NULL).head)
    case u: Utf8 => u.toString
    case uuid: UUID => uuid.toString
    case enum: GenericEnumSymbol[_] => enum.toString
    case instant: Instant => customInstantDefault(instant)
    case fixed: GenericFixed => fixed.bytes()
    case bd: BigDecimal => bd.toString()
    case byteBuffer: ByteBuffer if schema.getLogicalType.isInstanceOf[Decimal] =>
      val decimalConversion = new Conversions.DecimalConversion
      val bd = decimalConversion.fromBytes(byteBuffer, schema, schema.getLogicalType)
      java.lang.Double.valueOf(bd.doubleValue)
    case byteBuffer: ByteBuffer => byteBuffer.array()
    case x: String => x
    case x: scala.Long => java.lang.Long.valueOf(x)
    case x: scala.Boolean => java.lang.Boolean.valueOf(x)
    case x: scala.Int => java.lang.Integer.valueOf(x)
    case x: scala.Double => java.lang.Double.valueOf(x)
    case x: scala.Float => java.lang.Float.valueOf(x)
    case x: Map[_,_] => x.asJava
    case x: Seq[_] => customArrayDefault(x, schema)
    case x: Set[_] => customArrayDefault(x.toList, schema)
    case shapeless.Inl(x) => apply(x, schema)
    case x if isValueClass(x.getClass) => // must be tested before `Product` because most value classes are declared as case class.
      val field: Field = x.getClass.getDeclaredFields.head
      field.setAccessible(true) // can be private, like in Refined for example
      apply(field.get(x), schema)
    case p: Product => customDefault(p, schema)
    case v if isScalaEnumeration(v) => customScalaEnumDefault(value)
    case _ => value.asInstanceOf[AnyRef]
  }

  private[avro4s] def isValueClass(clazz: Class[_]): Boolean =
    try {
      import scala.reflect.runtime.universe
      val mirror = universe.runtimeMirror(Thread.currentThread().getContextClassLoader)
      mirror.staticClass(clazz.getName).isDerivedValueClass
    } catch {
      case NonFatal(_) => false
    }

}
