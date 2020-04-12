package com.sksamuel.avro4s

import java.util.Collections

import com.sksamuel.avro4s.ShapelessCoproducts._
import org.apache.avro.Schema
import shapeless.{:+:, CNil, Coproduct, Inl, Inr}

import scala.reflect.runtime.universe._

trait ShapelessCoproductEncoders {

  implicit final val CNilEncoder: Encoder[CNil] = ShapelessCoproducts.CNilCodec

  implicit final def coproductEncoder[H: WeakTypeTag: Manifest, T <: Coproduct](
      implicit encoderH: Encoder[H],
      encoderT: Encoder[T]): Encoder[H :+: T] =
    new Encoder[H :+: T] {

      val schemaFor: SchemaFor[H :+: T] = SchemaFor.coproductSchemaFor(encoderH.schemaFor, encoderT.schemaFor)

      def encode(value: H :+: T): AnyRef = value match {
        case Inl(h) => encoderH.encode(h)
        case Inr(t) => encoderT.encode(t)
      }

      override def withSchema(schemaFor: SchemaFor[H :+: T]): Encoder[H :+: T] =
        coproductEncoder[H, T](
          implicitly[WeakTypeTag[H]],
          implicitly[Manifest[H]],
          withSelectedSubSchema(encoderH, schemaFor),
          withFullSchema(encoderT, schemaFor)
        )
    }
}

trait ShapelessCoproductDecoders {

  implicit val CNilDecoder: Decoder[CNil] = ShapelessCoproducts.CNilCodec

  implicit final def coproductDecoder[H: WeakTypeTag: Manifest, T <: Coproduct](
      implicit decoderH: Decoder[H],
      decoderT: Decoder[T]): Decoder[H :+: T] =
    new Decoder[H :+: T] {

      val schemaFor: SchemaFor[H :+: T] = SchemaFor.coproductSchemaFor(decoderH.schemaFor, decoderT.schemaFor)

      private implicit val elementDecoder: PartialFunction[Any, H] = TypeGuardedDecoding.guard(decoderH)

      def decode(value: Any): H :+: T =
        if (elementDecoder.isDefinedAt(value)) Inl(elementDecoder(value)) else Inr(decoderT.decode(value))

      override def withSchema(schemaFor: SchemaFor[H :+: T]): Decoder[H :+: T] =
        coproductDecoder[H, T](
          implicitly[WeakTypeTag[H]],
          implicitly[Manifest[H]],
          withSelectedSubSchema(decoderH, schemaFor),
          withFullSchema(decoderT, schemaFor)
        )
    }
}

object ShapelessCoproducts {

  object CNilCodec extends Codec[CNil] {
    val schemaFor: SchemaFor[CNil] = SchemaFor(Schema.createUnion(Collections.emptyList[Schema]()))

    def encode(value: CNil): AnyRef = sys.error(s"Unexpected value '$value' of type CNil (that doesn't exist)")

    def decode(value: Any): CNil = sys.error(s"Unable to decode value '$value'")

    override def withSchema(schemaFor: SchemaFor[CNil]): Codec[CNil] = this
  }

  private[avro4s] def withSelectedSubSchema[T[_], V](aware: SchemaAware[T, V], schemaFor: SchemaFor[_])(
      implicit m: Manifest[V]): T[V] = {
    val schema = schemaFor.schema
    val fullName = NameExtractor(manifest.runtimeClass).fullName
    val subSchemaFor = SchemaFor[V](SchemaHelper.extractTraitSubschema(fullName, schema))
    aware.withSchema(subSchemaFor)
  }

  private[avro4s] def withFullSchema[T[_], V](aware: SchemaAware[T, V], schemaFor: SchemaFor[_]): T[V] =
    aware.withSchema(schemaFor.map(identity))
}
