package com.sksamuel.avro4s.schemas

import com.sksamuel.avro4s.avroutils.SchemaHelper
import com.sksamuel.avro4s.typeutils.{Annotations, Names}
import com.sksamuel.avro4s.{SchemaConfiguration, SchemaFor}
import magnolia.CaseClass
import org.apache.avro.{Schema, SchemaBuilder}

import scala.jdk.CollectionConverters._

object Records:

  def schema[T](ctx: CaseClass[SchemaFor, T]): SchemaFor[T] = {

    val annos = Annotations(ctx.annotations, ctx.typeAnnotations)
    val naming = Names(ctx.typeInfo, annos)
    val error = false

    val record = Schema.createRecord(
      naming.name.replaceAll("[^a-zA-Z0-9_]", ""),
      annos.doc.orNull,
      naming.namespace.replaceAll("[^a-zA-Z0-9_.]", ""),
      error
    )

    val fields = ctx.params.toList.flatMap { param =>
      val fieldAnnos = new Annotations(param.annotations, param.typeAnnotations)
      if (fieldAnnos.transient) None
      else {
        val doc = annos.doc //.orElse(valueTypeDoc).orNull
        //        val doc = valueTypeDoc(ctx, param)
        Some(buildSchemaField(param, fieldAnnos, record.getNamespace, doc))
      }
    }
    record.setFields(fields.toList.asJava)

    annos.aliases.foreach(record.addAlias)
    annos.props.foreach { case (k, v) => record.addProp(k: String, v: AnyRef) }

    new SchemaFor[T] {
      override def schema(config: SchemaConfiguration): Schema = record
    }
  }

  private def buildSchemaField[T](param: CaseClass.Param[SchemaFor, T],
                                  //                                                        baseSchema: Schema,
                                  fieldAnnos: Annotations,
                                  containingNamespace: String,
                                  //                                                        fieldMapper: FieldMapper,
                                  valueTypeDoc: Option[String]): Schema.Field = {

    val baseSchema = param.typeclass.schema(null)

    val name = param.label
    val doc = fieldAnnos.doc.orElse(valueTypeDoc).orNull
    val aliases = fieldAnnos.aliases
    val props = fieldAnnos.props

    // the field may have its own namespace from an avro annotation
    val fieldNamespace = fieldAnnos.namespace.getOrElse(containingNamespace)

    // if we have annotated with @AvroFixed then we override the type and change it to a Fixed schema
    // if someone puts @AvroFixed on a complex type, it makes no sense, but that's their cross to bear
    val schema = fieldAnnos.fixed.fold(baseSchema) { size =>
      SchemaBuilder.fixed(name).doc(doc).namespace(fieldNamespace).size(size)
    }

    // for a union the type that has a default must be first (including null as an explicit default)
    // if there is no default then we'll move null to head (if present)
    // otherwise left as is
    // todo magnolia for scala 3 doesn't support defaults yet
    val schemaWithOrderedUnion = schema
    //    val schemaWithOrderedUnion = (schemaWithPossibleNull.getType, encodedDefault) match {
    //      case (Schema.Type.UNION, null) => SchemaHelper.moveNullToHead(schemaWithPossibleNull)
    //      case (Schema.Type.UNION, JsonProperties.NULL_VALUE) => SchemaHelper.moveNullToHead(schemaWithPossibleNull)
    //      case (Schema.Type.UNION, defaultValue) => SchemaHelper.moveDefaultToHead(schemaWithPossibleNull, defaultValue)
    //      case _ => schemaWithPossibleNull
    //    }

    // the field can override the containingNamespace if the AvroNamespace annotation is present on the field
    // we may have annotated our field with @AvroNamespace so this containingNamespace should be applied
    // to any schemas we have generated for this field
    val schemaWithResolvedNamespace = fieldAnnos.namespace
      .map(SchemaHelper.overrideNamespace(schemaWithOrderedUnion, _))
      .getOrElse(schemaWithOrderedUnion)

    val field = new Schema.Field(name, schemaWithResolvedNamespace, doc)
    props.foreach { case (k, v) => field.addProp(k, v: AnyRef) }
    aliases.foreach(field.addAlias)
    field
  }
//
//    val default: Option[AnyRef] = if (extractor.nodefault) None else param.default.asInstanceOf[Option[AnyRef]]

//
//    // the name could have been overriden with @AvroName, and then must be encoded with the field mapper
//    val name = extractor.name.getOrElse(fieldMapper.to(param.label))
//
//    // the default value may be none, in which case it was not defined, or Some(null), in which case it was defined
//    // and set to null, or something else, in which case it's a non null value
//    val encodedDefault: AnyRef = default match {
//      case None        => null
//      case Some(None)  => JsonProperties.NULL_VALUE
//      case Some(null)  => JsonProperties.NULL_VALUE
//      case Some(other) => DefaultResolver(other, baseSchema)
//    }
//
//
//    // if our default value is null, then we should change the type to be nullable even if we didn't use option
//    val schemaWithPossibleNull = if (default.contains(null) && schema.getType != Schema.Type.UNION) {
//      SchemaBuilder.unionOf().`type`(schema).and().`type`(Schema.create(Schema.Type.NULL)).endUnion()
//    } else schema
//

//
//    val field = encodedDefault match {
//      case null => new Schema.Field(name, schemaWithResolvedNamespace, doc)
//      case CustomUnionDefault(_, m) =>
//        new Schema.Field(name, schemaWithResolvedNamespace, doc, m)
//      case CustomEnumDefault(m) =>
//        new Schema.Field(name, schemaWithResolvedNamespace, doc, m)
//      case CustomUnionWithEnumDefault(_, _, m) => new Schema.Field(name, schemaWithResolvedNamespace, doc, m)
//      case _                                   => new Schema.Field(name, schemaWithResolvedNamespace, doc, encodedDefault)
//    }
//
