package com.sksamuel.avro4s.schemas

import com.sksamuel.avro4s.avroutils.SchemaHelper
import com.sksamuel.avro4s.{Avro4sException, FieldMapper, SchemaFor}
import org.apache.avro.{Schema, SchemaBuilder}

trait OptionSchemas {

  given SchemaFor[None.type] = NoneSchemaFor

  given[T](using schemaFor: SchemaFor[T]): SchemaFor[Option[T]] = new SchemaFor[Option[T]] {
    override def schema: Schema = {
      val rhs: Schema = schemaFor.schema
      if (rhs.isUnion)
        val types = rhs.getTypes
        types.add(0, Schema.create(Schema.Type.NULL))
        Schema.createUnion(types)
      else
        Schema.createUnion(Schema.create(Schema.Type.NULL), rhs)
    }
  }
}

object NoneSchemaFor extends SchemaFor[None.type] :
  private val s = SchemaBuilder.builder.nullType
  override def schema: Schema = s