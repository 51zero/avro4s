package com.sksamuel.avro4s

import com.sksamuel.avro4s.SchemaUpdate.{FullSchemaUpdate, NamespaceUpdate, NoUpdate}
import com.sksamuel.avro4s.TypeUnionEntry._
import com.sksamuel.avro4s.TypeUnions._
import magnolia.{SealedTrait, Subtype}
import org.apache.avro.Schema
import org.apache.avro.generic.GenericContainer

class TypeUnionCodec[T](ctx: SealedTrait[Codec, T],
                        val schema: Schema,
                        codecByName: Map[String, EntryDecoder[T]],
                        codecBySubtype: Map[Subtype[Codec, T], EntryEncoder[T]])
    extends Codec[T]
    with NamespaceAware[Codec[T]] {

  def withNamespace(namespace: String): Codec[T] = TypeUnions.codec(ctx, NamespaceUpdate(namespace))

  override def withSchema(schemaFor: SchemaForV2[T]): Codec[T] = {
    validateNewSchema(schemaFor)
    TypeUnions.codec(ctx, FullSchemaUpdate(schemaFor))
  }

  def encode(value: T): AnyRef = encodeUnion(ctx, codecBySubtype, value)

  def decode(value: Any): T = decodeUnion(ctx, codecByName, value)
}

class TypeUnionEncoder[T](ctx: SealedTrait[EncoderV2, T],
                          val schema: Schema,
                          encoderBySubtype: Map[Subtype[EncoderV2, T], EntryEncoder[T]])
    extends EncoderV2[T]
    with NamespaceAware[EncoderV2[T]] {

  def withNamespace(namespace: String): EncoderV2[T] = TypeUnions.encoder(ctx, NamespaceUpdate(namespace))

  override def withSchema(schemaFor: SchemaForV2[T]): EncoderV2[T] = {
    validateNewSchema(schemaFor)
    TypeUnions.encoder(ctx, FullSchemaUpdate(schemaFor))
  }

  def encode(value: T): AnyRef = encodeUnion(ctx, encoderBySubtype, value)
}

class TypeUnionDecoder[T](ctx: SealedTrait[DecoderV2, T],
                          val schema: Schema,
                          decoderByName: Map[String, EntryDecoder[T]])
    extends DecoderV2[T]
    with NamespaceAware[DecoderV2[T]] {

  def withNamespace(namespace: String): DecoderV2[T] = TypeUnions.decoder(ctx, NamespaceUpdate(namespace))

  override def withSchema(schemaFor: SchemaForV2[T]): DecoderV2[T] = {
    validateNewSchema(schemaFor)
    TypeUnions.decoder(ctx, FullSchemaUpdate(schemaFor))
  }

  def decode(value: Any): T = decodeUnion(ctx, decoderByName, value)
}

object TypeUnions {

  private[avro4s] def encodeUnion[Typeclass[_], T](ctx: SealedTrait[Typeclass, T],
                                                   encoderBySubtype: Map[Subtype[Typeclass, T], EntryEncoder[T]],
                                                   value: T): AnyRef =
    ctx.dispatch(value)(subtype => encoderBySubtype(subtype).encodeSubtype(value))

  private[avro4s] def decodeUnion[Typeclass[_], T](ctx: SealedTrait[Typeclass, T],
                                                   decoderByName: Map[String, EntryDecoder[T]],
                                                   value: Any) =
    value match {
      case container: GenericContainer =>
        val schemaName = container.getSchema.getFullName
        val codecOpt = decoderByName.get(schemaName)
        if (codecOpt.isDefined) {
          codecOpt.get.decodeSubtype(container)
        } else {
          val schemaNames = decoderByName.keys.toSeq.sorted.mkString("[", ", ", "]")
          sys.error(s"Could not find schema $schemaName in type union schemas $schemaNames")
        }
      case _ => sys.error(s"Unsupported type $value in type union decoder")
    }

  trait EntryDecoder[T] {
    def decodeSubtype(value: Any): T
  }

  trait EntryEncoder[T] {
    def encodeSubtype(value: T): AnyRef
  }

  trait EntryCodec[T] extends EntryDecoder[T] with EntryEncoder[T]

  def codec[T](ctx: SealedTrait[Codec, T], update: SchemaUpdate): Codec[T] =
    create(ctx, update, new CodecBuilder[T])

  def encoder[T](ctx: SealedTrait[EncoderV2, T], update: SchemaUpdate): EncoderV2[T] =
    create(ctx, update, new EncoderBuilder[T])

  def decoder[T](ctx: SealedTrait[DecoderV2, T], update: SchemaUpdate): DecoderV2[T] =
    create(ctx, update, new DecoderBuilder[T])

  def schema[T](ctx: SealedTrait[SchemaForV2, T], update: SchemaUpdate, fieldMapper: FieldMapper): SchemaForV2[T] =
    create(ctx, update, new SchemaForBuilder[T](fieldMapper))

  private trait Builder[Typeclass[_], T, Entry[_]] {
    def entryConstructor: (Subtype[Typeclass, T], SchemaUpdate) => Entry[T]

    def entrySchema: Entry[T] => Schema

    def constructor: (SealedTrait[Typeclass, T], Seq[Entry[T]], Schema) => Typeclass[T]
  }

  private class CodecBuilder[T] extends Builder[Codec, T, UnionEntryCodec] {

    val entryConstructor = new UnionEntryCodec[T](_, _)

    val entrySchema = _.schema

    val constructor = { (ctx, subtypeCodecs, schema) =>
      val codecByName = subtypeCodecs.map(c => c.fullName -> c).toMap
      val codecBySubtype = subtypeCodecs.map(c => c.st -> c).toMap
      new TypeUnionCodec(ctx, schema, codecByName, codecBySubtype)
    }
  }

  private class EncoderBuilder[T] extends Builder[EncoderV2, T, UnionEntryEncoder] {
    val entryConstructor = new UnionEntryEncoder[T](_, _)

    val entrySchema = _.schema

    val constructor = { (ctx, subtypeEncoder, schema) =>
      val encoderBySubtype = subtypeEncoder.map(c => c.st -> c).toMap
      new TypeUnionEncoder(ctx, schema, encoderBySubtype)
    }
  }

  private class DecoderBuilder[T] extends Builder[DecoderV2, T, UnionEntryDecoder] {
    val entryConstructor = new UnionEntryDecoder[T](_, _)

    val entrySchema = _.schema

    val constructor = { (ctx, subtypeDecoder, schema) =>
      val decoderByName = subtypeDecoder.map(decoder => decoder.fullName -> decoder).toMap
      new TypeUnionDecoder(ctx, schema, decoderByName)
    }
  }

  private class SchemaForBuilder[T](fieldMapper: FieldMapper) extends Builder[SchemaForV2, T, SchemaForV2] {

    def entryConstructor: (Subtype[SchemaForV2, T], SchemaUpdate) => SchemaForV2[T] = { (st, u) =>
      u match {
        case NamespaceUpdate(ns) =>
          val oldSchemaFor = st.typeclass
          SchemaForV2(SchemaHelper.overrideNamespace(oldSchemaFor.schema, ns), fieldMapper)

        case FullSchemaUpdate(schemaFor) => schemaFor.forType[T]
        case NoUpdate                    => st.typeclass.forType[T]
      }
    }

    val entrySchema = _.schema

    def constructor: (SealedTrait[SchemaForV2, T], Seq[SchemaForV2[T]], Schema) => SchemaForV2[T] =
      (_, schemaFors, _) => SchemaForV2[T](SchemaHelper.createSafeUnion(schemaFors.map(_.schema): _*), fieldMapper)
  }

  private def create[Typeclass[_], T, E[_]](ctx: SealedTrait[Typeclass, T],
                                            update: SchemaUpdate,
                                            builder: Builder[Typeclass, T, E]): Typeclass[T] = {
    val extendedUpdate = update match {
      case NoUpdate =>
        val ns = new AnnotationExtractors(ctx.annotations).namespace
        ns.fold[SchemaUpdate](NoUpdate)(NamespaceUpdate.apply)
      case _ => update
    }
    val subtypeEntries = buildSubtypeEntries(ctx, extendedUpdate, builder)
    val schema = buildSchema(ctx, extendedUpdate, subtypeEntries, builder)
    builder.constructor(ctx, subtypeEntries, schema)
  }

  private[avro4s] def buildSubtypeEntries[Typeclass[_], T, E[_]](ctx: SealedTrait[Typeclass, T],
                                                                 overrides: SchemaUpdate,
                                                                 builder: Builder[Typeclass, T, E]): Seq[E[T]] = {

    def subtypeOverride(st: Subtype[Typeclass, T]) = overrides match {
      case FullSchemaUpdate(schemaFor) =>
        val schema = schemaFor.schema
        val fieldMapper = schemaFor.fieldMapper
        val nameExtractor = NameExtractor(st.typeName, st.annotations ++ ctx.annotations)
        val subtraitSchema =
          SchemaForV2(SchemaHelper.extractTraitSubschema(nameExtractor.fullName, schema), fieldMapper)
        FullSchemaUpdate(subtraitSchema)
      case _ => overrides
    }

    ctx.subtypes.map { st: Subtype[Typeclass, T] =>
      builder.entryConstructor(st, subtypeOverride(st))
    }
  }

  private[avro4s] def validateNewSchema[T](schemaFor: SchemaForV2[T]) = {
    val newSchema = schemaFor.schema
    require(newSchema.getType == Schema.Type.UNION,
            s"Schema type for record codecs must be UNION, received ${newSchema.getType}")
  }

  def buildSchema[Typeclass[_], T, E[_]](ctx: SealedTrait[Typeclass, T],
                                         update: SchemaUpdate,
                                         entries: Seq[E[T]],
                                         builder: Builder[Typeclass, T, E]): Schema = {
    update match {
      case FullSchemaUpdate(s) => s.schema
      case _ =>
        val entryMap = ctx.subtypes.zip(entries).toMap
        val sortedSubtypes = {
          def priority(st: Subtype[Typeclass, T]) =
            new AnnotationExtractors(st.annotations).sortPriority.getOrElse(0.0f)
          ctx.subtypes.sortBy(st => (priority(st), st.typeName.full))
        }
        SchemaHelper.createSafeUnion(sortedSubtypes.map(entryMap).map(builder.entrySchema): _*)
    }
  }
}
