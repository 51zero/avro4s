package com.sksamuel.avro4s.avroutils

import com.sksamuel.avro4s.{Avro4sConfigurationException, CustomUnionDefault, CustomUnionWithEnumDefault, FieldMapper}
import org.apache.avro.generic.GenericData
import org.apache.avro.util.Utf8
import org.apache.avro.{JsonProperties, Schema, SchemaBuilder}

import scala.util.matching.Regex

object SchemaHelper {

  import scala.collection.JavaConverters._

  def matchPrimitiveName(fullName: String): Option[Schema] = fullName match {
    case "java.lang.Integer" => Some(Schema.create(Schema.Type.INT))
    case "java.lang.String" => Some(Schema.create(Schema.Type.STRING))
    case "java.lang.Long" => Some(Schema.create(Schema.Type.LONG))
    case "java.lang.Boolean" => Some(Schema.create(Schema.Type.BOOLEAN))
    case "java.lang.Double" => Some(Schema.create(Schema.Type.DOUBLE))
    case "java.lang.Float" => Some(Schema.create(Schema.Type.FLOAT))
    case _ => None
  }

  private val arrayTypeNamePattern: Regex = "scala.collection.immutable.::(__B)?".r

  /**
    * Given the full name of a record, locates the appropriate sub schema from the given union.
    */
  def extractTraitSubschema(fullName: String, union: Schema): Schema = matchPrimitiveName(fullName) getOrElse {
    if (union.getType != Schema.Type.UNION)
      throw new Avro4sConfigurationException(s"Can only extract subschemas from a UNION but was given $union")

    val types = union.getTypes
    val size = types.size

    if (size == 0) throw new Avro4sConfigurationException(s"Cannot extract subschema from empty UNION $union")

    // if we are looking for an array type then find "array" first
    // this is totally not FP but what the heck it's late and it's perfectly valid
    arrayTypeNamePattern.findFirstMatchIn(fullName) match {
      case Some(_) =>
        return types.asScala.find(_.getType == Schema.Type.ARRAY).getOrElse {
          throw new Avro4sConfigurationException(s"Could not find array type to match $fullName")
        }
      case None =>
    }

    // Finds the matching schema and keeps track a null type if any.
    // If the schema is size 2, and one of them is null, then the other type is returned, regardless of its name.
    // See https://github.com/sksamuel/avro4s/issues/268
    var result: Schema = null
    var nullIndex: Int = -1
    var i = 0
    while
      i < size && result == null
    do
      val s = types.get(i)
      if (s.getFullName == fullName) result = s
      else if (s.getType == Schema.Type.NULL) nullIndex = i
      i = i + 1

    if (result != null) { // Return the name based match
      result
    } else if (nullIndex != -1 && size == 2) { // Return the non null type.
      types.get(i - nullIndex)
    } else {
      throw new Avro4sConfigurationException(s"Cannot find subschema for type [$fullName] in ${union.getTypes}")
    }
  }

  /**
    * Throws if the given schema is not suitable for use in an either.
    */
  def validateEitherSchema(schema: Schema): Unit = {
    if (schema.getType != Schema.Type.UNION)
      throw new Avro4sConfigurationException(
        s"Schema type for either encoders / decoders must be UNION, received $schema")
    if (schema.getTypes.size() != 2)
      throw new Avro4sConfigurationException(
        s"Schema for either encoders / decoders must be a UNION of to types, received $schema")
  }

  /**
    * Returns the subschema used for a Left in an Either.
    */
  def extractEitherLeftSchema(schema: Schema): Schema = {
    validateEitherSchema(schema)
    schema.getTypes.get(0)
  }

  /**
    * Returns the subschema used for a Right in an Either.
    */
  def extractEitherRightSchema(schema: Schema): Schema = {
    validateEitherSchema(schema)
    schema.getTypes.get(1)
  }

  /**
    * Requires a UNION schema and will attempt to find a subschema that
    * matches the default value type. If one matches it will move that
    * schema to the head, as required by the spec.
    */
  def moveDefaultToHead(schema: Schema, default: Any): Schema = {
    if (schema.getType != Schema.Type.UNION)
      throw new Avro4sConfigurationException(s"Can handle UNION schemas only, but got $schema")
    val defaultType = default match {
      case _: String => Schema.Type.STRING
      case x if x.getClass.isEnum => Schema.Type.ENUM
      case _: Utf8 => Schema.Type.STRING
      case _: Long => Schema.Type.LONG
      case _: Int => Schema.Type.INT
      case _: Boolean => Schema.Type.BOOLEAN
      case _: Float => Schema.Type.FLOAT
      case _: Double => Schema.Type.DOUBLE
      case _: Array[Byte] => Schema.Type.BYTES
      case _: GenericData.EnumSymbol => Schema.Type.ENUM
      case _: java.util.Collection[_] => Schema.Type.ARRAY
      case _: java.util.Map[_, _] => Schema.Type.MAP
      case JsonProperties.NULL_VALUE => Schema.Type.NULL
      //      case CustomEnumDefault(_) => Schema.Type.ENUM
      case other => other
    }

    val (first, rest) = schema.getTypes.asScala.partition { t =>
      defaultType match {
        case CustomUnionDefault(name, _) => name == t.getName
        case CustomUnionWithEnumDefault(name, default, _) =>
                 name == t.getName
        case _ => t.getType == defaultType
      }
    }
    val result = Schema.createUnion(first.headOption.toSeq ++ rest: _*)
    schema.getObjectProps.asScala.foreach { case (k, v) => result.addProp(k, v) }
    result
  }

  /**
    * Requires a UNION schema and will move the null schema to the head if it exists.
    * Otherwise returns the UNION as is.
    */
  def moveNullToHead(schema: Schema): Schema = {
    if (schema.getType != Schema.Type.UNION)
      throw new Avro4sConfigurationException(s"Can order types only in UNION, but got $schema")
    if (schema.getTypes.asScala.exists(_.getType == Schema.Type.NULL)) {
      val (nulls, rest) = schema.getTypes.asScala.partition(_.getType == Schema.Type.NULL)
      val result = Schema.createUnion(nulls.headOption.toSeq ++ rest: _*)
      schema.getObjectProps.asScala.foreach { case (k, v) => result.addProp(k, v) }
      result
    } else schema
  }

  /**
    * Creates a union schema type, with nested unions extracted, and duplicate nulls stripped.
    *
    * Union schemas can't contain other union schemas as a direct child,
    * so whenever we create a union, we need to check if our children are unions and flatten
    *
    * For example, an Option[SealedTrait], would result in a union of a union, so this must be
    * flattened into a single union.
    */
  def createSafeUnion(schemas: Schema*): Schema = {
    val flattened = schemas.flatMap(schema => scala.util.Try(schema.getTypes.asScala).getOrElse(Seq(schema)))
    val (nulls, rest) = flattened.partition(_.getType == Schema.Type.NULL)
    Schema.createUnion(nulls.headOption.toSeq ++ rest: _*)
  }

  def setError(schema: Schema): Schema = {
    schema.getType match {
      case Schema.Type.RECORD =>
        val fields = schema.getFields.asScala.map { field =>
          new Schema.Field(
            field.name(),
            field.schema(),
            field.doc,
            field.defaultVal,
            field.order)
        }
        val copy = Schema.createRecord(schema.getName, schema.getDoc, schema.getNamespace, true, fields.asJava)
        schema.getAliases.asScala.foreach(copy.addAlias)
        schema.getObjectProps.asScala.foreach { case (k, v) => copy.addProp(k, v) }
        copy
      case _ => schema
    }
  }

  def mapNames(schema: Schema, mapper: FieldMapper): Schema = {
    schema.getType match {
      case Schema.Type.RECORD =>
        val fields = schema.getFields.asScala.map { field =>
          new Schema.Field(
            mapper.to(field.name()),
            mapNames(field.schema(), mapper),
            field.doc,
            field.defaultVal,
            field.order)
        }
        val copy = Schema.createRecord(schema.getName, schema.getDoc, schema.getNamespace, schema.isError, fields.asJava)
        schema.getAliases.asScala.foreach(copy.addAlias)
        schema.getObjectProps.asScala.foreach { case (k, v) => copy.addProp(k, v) }
        copy
      case Schema.Type.UNION => Schema.createUnion(schema.getTypes.asScala.map(mapNames(_, mapper)).asJava)
      case Schema.Type.ENUM => Schema.createEnum(schema.getName, schema.getDoc, schema.getNamespace, schema.getEnumSymbols)
      case Schema.Type.FIXED => Schema.createFixed(schema.getName, schema.getDoc, schema.getNamespace, schema.getFixedSize)
      case Schema.Type.MAP => Schema.createMap(mapNames(schema.getValueType, mapper))
      case Schema.Type.ARRAY => Schema.createArray(mapNames(schema.getElementType, mapper))
      case _ => schema
    }
  }

  /**
    * Takes an Avro schema, and overrides the namespace of that schema with the given namespace.
    */
  def overrideNamespace(schema: Schema, namespace: String): Schema = {
    schema.getType match {
      case Schema.Type.RECORD =>
        val fields = schema.getFields.asScala.map { field =>
          new Schema.Field(
            field.name(),
            overrideNamespace(field.schema(), namespace),
            field.doc,
            field.defaultVal,
            field.order)
        }
        val copy = Schema.createRecord(schema.getName, schema.getDoc, namespace, schema.isError, fields.asJava)
        schema.getAliases.asScala.foreach(copy.addAlias)
        schema.getObjectProps.asScala.foreach { case (k, v) => copy.addProp(k, v) }
        copy
      case Schema.Type.UNION => Schema.createUnion(schema.getTypes.asScala.map(overrideNamespace(_, namespace)).asJava)
      case Schema.Type.ENUM => Schema.createEnum(schema.getName, schema.getDoc, namespace, schema.getEnumSymbols)
      case Schema.Type.FIXED => Schema.createFixed(schema.getName, schema.getDoc, namespace, schema.getFixedSize)
      case Schema.Type.MAP => Schema.createMap(overrideNamespace(schema.getValueType, namespace))
      case Schema.Type.ARRAY => Schema.createArray(overrideNamespace(schema.getElementType, namespace))
      case _ => schema
    }
  }
}
