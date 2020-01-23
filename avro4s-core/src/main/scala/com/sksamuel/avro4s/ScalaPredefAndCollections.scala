package com.sksamuel.avro4s

import java.util

import com.sksamuel.avro4s.ScalaPredefAndCollections._
import org.apache.avro.{Schema, SchemaBuilder}

import scala.collection.JavaConverters._
import scala.collection.generic.CanBuildFrom
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import scala.language.implicitConversions

trait ScalaPredefAndCollectionCodecs {

  implicit val NoneCodec: Codec[None.type] = ScalaPredefAndCollections.NoneCodec

  implicit def optionCodec[T](implicit codec: Codec[T]): Codec[Option[T]] = new Codec[Option[T]] {

    val schema: Schema = optionSchema(codec)

    def encode(value: Option[T]): AnyRef = encodeOption(codec, value)

    def decode(value: Any): Option[T] = decodeOption(codec, value)

    override def withSchema(schemaFor: SchemaForV2[Option[T]]): Codec[Option[T]] =
      optionCodec(codec.withSchema(extractOptionSchema(schemaFor)))
  }

  implicit def eitherCodec[A: Manifest: WeakTypeTag, B: Manifest: WeakTypeTag](
      implicit leftCodec: Codec[A],
      rightCodec: Codec[B]): Codec[Either[A, B]] =
    new Codec[Either[A, B]] {
      val schema: Schema = SchemaHelper.createSafeUnion(leftCodec.schema, rightCodec.schema)

      def encode(value: Either[A, B]): AnyRef = encodeEither(value)

      private implicit val leftGuard: PartialFunction[Any, A] = TypeGuardedDecoding.guard(leftCodec)
      private implicit val rightGuard: PartialFunction[Any, B] = TypeGuardedDecoding.guard(rightCodec)

      def decode(value: Any): Either[A, B] = decodeEither(value, manifest[A], manifest[B])

      override def withSchema(schemaFor: SchemaForV2[Either[A, B]]): Codec[Either[A, B]] =
        eitherCodec(
          manifest[A],
          weakTypeTag[A],
          manifest[B],
          weakTypeTag[B],
          leftCodec.withSchema(extractEitherLeftSchema(schemaFor)),
          rightCodec.withSchema(extractEitherRightSchema(schemaFor))
        )
    }

  implicit def arrayCodec[T: ClassTag](implicit codec: Codec[T]): Codec[Array[T]] = new Codec[Array[T]] {
    def schema: Schema = arraySchema(codec)

    def encode(value: Array[T]): AnyRef = encodeArray(codec, value)

    def decode(value: Any): Array[T] = decodeArray(codec, value)

    override def withSchema(schemaFor: SchemaForV2[Array[T]]): Codec[Array[T]] =
      arrayCodec(implicitly[ClassTag[T]], codec.withSchema(extractArrayElementSchema(schemaFor)))
  }

  private def iterableCodec[C[X] <: Iterable[X], T](codec: Codec[T])(
    implicit cbf: CanBuildFrom[Nothing, T, C[T]]): Codec[C[T]] =
    new Codec[C[T]] {
      val schema: Schema = iterableSchema(codec)

      def encode(value: C[T]): AnyRef = encodeIterable(codec, value)

      def decode(value: Any): C[T] = decodeIterable(codec, value)

      override def withSchema(schemaFor: SchemaForV2[C[T]]): Codec[C[T]] =
        iterableCodec(codec.withSchema(extractIterableElementSchema(schemaFor)))
    }

  implicit def listCodec[T](implicit codec: Codec[T]): Codec[List[T]] = iterableCodec(codec)
  implicit def mutableSeqCodec[T](implicit codec: Codec[T]): Codec[scala.collection.mutable.Seq[T]] =
    iterableCodec(codec)
  implicit def seqCodec[T](implicit codec: Codec[T]): Codec[Seq[T]] = iterableCodec(codec)
  implicit def setCodec[T](implicit codec: Codec[T]): Codec[Set[T]] = iterableCodec(codec)
  implicit def vectorCodec[T](implicit codec: Codec[T]): Codec[Vector[T]] = iterableCodec(codec)

  implicit def mapCodec[T](implicit codec: Codec[T]): Codec[Map[String, T]] = new Codec[Map[String, T]] {
    val schema: Schema = mapSchema(codec)

    def encode(value: Map[String, T]): AnyRef = encodeMap(codec, value)

    def decode(value: Any): Map[String, T] = decodeMap(codec, value)

    override def withSchema(schemaFor: SchemaForV2[Map[String, T]]): Codec[Map[String, T]] =
      mapCodec(codec.withSchema(extractMapValueSchema(schemaFor)))
  }
}

trait ScalaPredefAndCollectionEncoders {

  implicit val NoneEncoder: EncoderV2[None.type] = ScalaPredefAndCollections.NoneCodec

  implicit def optionEncoder[T](implicit encoder: EncoderV2[T]): EncoderV2[Option[T]] = new EncoderV2[Option[T]] {

    val schema: Schema = optionSchema(encoder)

    def encode(value: Option[T]): AnyRef = encodeOption(encoder, value)

    override def withSchema(schemaFor: SchemaForV2[Option[T]]): EncoderV2[Option[T]] =
      optionEncoder(encoder.withSchema(extractOptionSchema(schemaFor)))
  }

  implicit def eitherEncoder[A, B](implicit leftEncoder: EncoderV2[A],
                                   rightEncoder: EncoderV2[B]): EncoderV2[Either[A, B]] =
    new EncoderV2[Either[A, B]] {
      val schema: Schema = SchemaHelper.createSafeUnion(leftEncoder.schema, rightEncoder.schema)

      def encode(value: Either[A, B]): AnyRef = encodeEither(value)

      override def withSchema(schemaFor: SchemaForV2[Either[A, B]]): EncoderV2[Either[A, B]] =
        eitherEncoder(leftEncoder.withSchema(extractEitherLeftSchema(schemaFor)),
                      rightEncoder.withSchema(extractEitherRightSchema(schemaFor)))
    }

  implicit def arrayEncoder[T: ClassTag](implicit encoder: EncoderV2[T]): EncoderV2[Array[T]] =
    new EncoderV2[Array[T]] {
      def schema: Schema = arraySchema(encoder)

      def encode(value: Array[T]): AnyRef = encodeArray(encoder, value)

      override def withSchema(schemaFor: SchemaForV2[Array[T]]): EncoderV2[Array[T]] =
        arrayEncoder(implicitly[ClassTag[T]], encoder.withSchema(extractArrayElementSchema(schemaFor)))
    }

  private def iterableEncoder[T, C[X] <: Iterable[X]](encoder: EncoderV2[T]): EncoderV2[C[T]] =
    new EncoderV2[C[T]] {
      val schema: Schema = iterableSchema(encoder)

      def encode(value: C[T]): AnyRef = encodeIterable(encoder, value)

      override def withSchema(schemaFor: SchemaForV2[C[T]]): EncoderV2[C[T]] =
        iterableEncoder(encoder.withSchema(extractIterableElementSchema(schemaFor)))
    }

  implicit def listEncoder[T](implicit encoder: EncoderV2[T]): EncoderV2[List[T]] = iterableEncoder(encoder)
  implicit def mutableSeqEncoder[T](implicit encoder: EncoderV2[T]): EncoderV2[scala.collection.mutable.Seq[T]] =
    iterableEncoder(encoder)
  implicit def seqEncoder[T](implicit encoder: EncoderV2[T]): EncoderV2[Seq[T]] = iterableEncoder(encoder)
  implicit def setEncoder[T](implicit encoder: EncoderV2[T]): EncoderV2[Set[T]] = iterableEncoder(encoder)
  implicit def vectorEncoder[T](implicit encoder: EncoderV2[T]): EncoderV2[Vector[T]] = iterableEncoder(encoder)

  implicit def mapEncoder[T](implicit encoder: EncoderV2[T]): EncoderV2[Map[String, T]] =
    new EncoderV2[Map[String, T]] {
      val schema: Schema = mapSchema(encoder)

      def encode(value: Map[String, T]): AnyRef = encodeMap(encoder, value)

      override def withSchema(schemaFor: SchemaForV2[Map[String, T]]): EncoderV2[Map[String, T]] =
        mapEncoder(encoder.withSchema(extractMapValueSchema(schemaFor)))
    }
}

trait ScalaPredefAndCollectionDecoders {

  implicit val NoneCodec: DecoderV2[None.type] = ScalaPredefAndCollections.NoneCodec

  implicit def optionDecoder[T](implicit decoder: DecoderV2[T]): DecoderV2[Option[T]] = new DecoderV2[Option[T]] {

    val schema: Schema = optionSchema(decoder)

    def decode(value: Any): Option[T] = decodeOption(decoder, value)

    override def withSchema(schemaFor: SchemaForV2[Option[T]]): DecoderV2[Option[T]] =
      optionDecoder(decoder.withSchema(extractOptionSchema(schemaFor)))
  }

  implicit def eitherDecoder[A: Manifest: WeakTypeTag, B: Manifest: WeakTypeTag](
      implicit leftDecoder: DecoderV2[A],
      rightDecoder: DecoderV2[B]): DecoderV2[Either[A, B]] =
    new DecoderV2[Either[A, B]] {
      val schema: Schema = SchemaHelper.createSafeUnion(leftDecoder.schema, rightDecoder.schema)

      private implicit val leftGuard: PartialFunction[Any, A] = TypeGuardedDecoding.guard(leftDecoder)
      private implicit val rightGuard: PartialFunction[Any, B] = TypeGuardedDecoding.guard(rightDecoder)

      def decode(value: Any): Either[A, B] = decodeEither(value, manifest[A], manifest[B])

      override def withSchema(schemaFor: SchemaForV2[Either[A, B]]): DecoderV2[Either[A, B]] =
        eitherDecoder(
          manifest[A],
          weakTypeTag[A],
          manifest[B],
          weakTypeTag[B],
          leftDecoder.withSchema(extractEitherLeftSchema(schemaFor)),
          rightDecoder.withSchema(extractEitherRightSchema(schemaFor))
        )
    }

  implicit def arrayDecoder[T: ClassTag](implicit decoder: DecoderV2[T]): DecoderV2[Array[T]] =
    new DecoderV2[Array[T]] {
      def schema: Schema = arraySchema(decoder)

      def decode(value: Any): Array[T] = decodeArray(decoder, value)

      override def withSchema(schemaFor: SchemaForV2[Array[T]]): DecoderV2[Array[T]] =
        arrayDecoder(implicitly[ClassTag[T]], decoder.withSchema(extractArrayElementSchema(schemaFor)))
    }

  private def iterableDecoder[T, C[X] <: Iterable[X]](decoder: DecoderV2[T])(
    implicit cbf: CanBuildFrom[Nothing, T, C[T]]): DecoderV2[C[T]] =
    new DecoderV2[C[T]] {
      val schema: Schema = iterableSchema(decoder)

      def decode(value: Any): C[T] = decodeIterable(decoder, value)

      override def withSchema(schemaFor: SchemaForV2[C[T]]): DecoderV2[C[T]] =
        iterableDecoder(decoder.withSchema(extractIterableElementSchema(schemaFor)))
    }


  implicit def listDecoder[T](implicit decoder: DecoderV2[T]): DecoderV2[List[T]] = iterableDecoder(decoder)
  implicit def mutableSeqDecoder[T](implicit decoder: DecoderV2[T]): DecoderV2[scala.collection.mutable.Seq[T]] =
    iterableDecoder(decoder)
  implicit def seqDecoder[T](implicit decoder: DecoderV2[T]): DecoderV2[Seq[T]] = iterableDecoder(decoder)
  implicit def setDecoder[T](implicit decoder: DecoderV2[T]): DecoderV2[Set[T]] = iterableDecoder(decoder)
  implicit def vectorDecoder[T](implicit decoder: DecoderV2[T]): DecoderV2[Vector[T]] = iterableDecoder(decoder)

  implicit def mapDecoder[T](implicit decoder: DecoderV2[T]): DecoderV2[Map[String, T]] =
    new DecoderV2[Map[String, T]] {
      val schema: Schema = mapSchema(decoder)

      def decode(value: Any): Map[String, T] = decodeMap(decoder, value)

      override def withSchema(schemaFor: SchemaForV2[Map[String, T]]): DecoderV2[Map[String, T]] =
        mapDecoder(decoder.withSchema(extractMapValueSchema(schemaFor)))
    }
}

object ScalaPredefAndCollections {

  private[avro4s] def optionSchema[A[_]](aware: SchemaAware[A, _]): Schema =
    SchemaForV2.optionSchema(SchemaForV2(aware.schema)).schema

  private[avro4s] def extractOptionSchema[T](schemaFor: SchemaForV2[Option[T]]): SchemaForV2[T] = {
    require(schemaFor.schema.getType == Schema.Type.UNION,
            s"Schema type for option encoders / decoders must be UNION, received ${schemaFor.schema.getType}")

    schemaFor.schema.getTypes.asScala.find(_.getType != Schema.Type.NULL) match {
      case Some(s) => SchemaForV2(s, schemaFor.fieldMapper)
      case None    => sys.error(s"Union schema ${schemaFor.schema} doesn't contain any non-null entries")
    }
  }

  @inline
  private[avro4s] def encodeOption[T](encoder: EncoderV2[T], value: Option[T]): AnyRef =
    if (value.isEmpty) null else encoder.encode(value.get)

  @inline
  private[avro4s] def decodeOption[T](decoder: DecoderV2[T], value: Any): Option[T] =
    if (value == null) None else Option(decoder.decode(value))

  @inline
  private[avro4s] def encodeEither[A, B](value: Either[A, B])(implicit leftEncoder: EncoderV2[A],
                                                              rightEncoder: EncoderV2[B]): AnyRef =
    value match {
      case Left(l)  => leftEncoder.encode(l)
      case Right(r) => rightEncoder.encode(r)
    }

  private[avro4s] def extractEitherLeftSchema[A, B](schemaFor: SchemaForV2[Either[A, B]]): SchemaForV2[A] = {
    require(schemaFor.schema.getType == Schema.Type.UNION,
            s"Schema type for either encoders / decoders must be UNION, received ${schemaFor.schema.getType}")
    require(schemaFor.schema.getTypes.size() == 2,
            s"Schema for either encoders / decoders must be a UNION of to types, received ${schemaFor.schema}")

    SchemaForV2(schemaFor.schema.getTypes.get(0), schemaFor.fieldMapper)
  }

  private[avro4s] def extractEitherRightSchema[A, B](schemaFor: SchemaForV2[Either[A, B]]): SchemaForV2[B] = {
    require(schemaFor.schema.getType == Schema.Type.UNION,
            s"Schema type for either encoders / decoders must be UNION, received ${schemaFor.schema.getType}")
    require(schemaFor.schema.getTypes.size() == 2,
            s"Schema for either encoders / decoders must be a UNION of to types, received ${schemaFor.schema}")

    SchemaForV2(schemaFor.schema.getTypes.get(1), schemaFor.fieldMapper)
  }

  @inline
  private[avro4s] def decodeEither[A, B](value: Any, manifestA: Manifest[A], manifestB: Manifest[B])(
      implicit leftGuard: PartialFunction[Any, A],
      rightGuard: PartialFunction[Any, B]): Either[A, B] =
    if (leftGuard.isDefinedAt(value)) {
      Left(leftGuard(value))
    } else if (rightGuard.isDefinedAt(value)) {
      Right(rightGuard(value))
    } else {
      val nameA = NameExtractor(manifestA.runtimeClass).fullName
      val nameB = NameExtractor(manifestB.runtimeClass).fullName
      sys.error(s"Could not decode $value into Either[$nameA, $nameB]")
    }

  object NoneCodec extends Codec[None.type] {
    val schema: Schema = SchemaBuilder.builder.nullType

    def encode(value: None.type): AnyRef = null

    def decode(value: Any): None.type =
      if (value == null) None else sys.error(s"Value $value is not null, but should be decoded to None")
  }

  private[avro4s] def iterableSchema[Typeclass[_], T](itemSchemaAware: SchemaAware[Typeclass, T]): Schema =
    SchemaBuilder.array().items(itemSchemaAware.schema)

  private[avro4s] def extractIterableElementSchema[C[X] <: Iterable[X], T](
      schemaFor: SchemaForV2[C[T]]): SchemaForV2[T] = {
    require(
      schemaFor.schema.getType == Schema.Type.ARRAY,
      s"Schema type for list / seq / vector encoders and decoders must be ARRAY, received ${schemaFor.schema.getType}"
    )
    SchemaForV2(schemaFor.schema.getElementType, schemaFor.fieldMapper)
  }

  @inline
  private[avro4s] def encodeIterable[T, C[X] <: Iterable[X]](encoder: EncoderV2[T], value: C[T]): AnyRef =
    value.map(encoder.encode).toList.asJava

  @inline
  private[avro4s] def decodeIterable[T, C[X] <: Iterable[X]](decoder: DecoderV2[T], value: Any)(
      implicit cbf: CanBuildFrom[Nothing, T, C[T]]): C[T] = value match {
    case array: Array[_]               => array.map(decoder.decode)(collection.breakOut)
    case list: java.util.Collection[_] => list.asScala.map(decoder.decode)(collection.breakOut)
    case list: Iterable[_]             => list.map(decoder.decode)(collection.breakOut)
    case other                         => sys.error("Unsupported array " + other)
  }

  private[avro4s] def arraySchema[TC[_], T](itemSchemaAware: SchemaAware[TC, T]): Schema =
    SchemaBuilder.array().items(itemSchemaAware.schema)

  private[avro4s] def extractArrayElementSchema[T](schemaFor: SchemaForV2[Array[T]]): SchemaForV2[T] = {
    require(schemaFor.schema.getType == Schema.Type.ARRAY,
            s"Schema type for array encoders / decoders must be ARRAY, received ${schemaFor.schema.getType}")
    SchemaForV2(schemaFor.schema.getElementType, schemaFor.fieldMapper)
  }

  @inline
  private[avro4s] def encodeArray[T](encoder: EncoderV2[T], value: Array[T]): AnyRef =
    value.map(encoder.encode).toList.asJava

  @inline
  private[avro4s] def decodeArray[T: ClassTag](decoder: DecoderV2[T], value: Any): Array[T] = value match {
    case array: Array[_]               => array.map(decoder.decode)
    case list: java.util.Collection[_] => list.asScala.map(decoder.decode).toArray
    case list: Iterable[_]             => list.map(decoder.decode).toArray
    case other                         => sys.error("Unsupported array " + other)
  }

  private[avro4s] def mapSchema[TC[_], T](valueSchemaAware: SchemaAware[TC, T]): Schema =
    SchemaBuilder.map().values(valueSchemaAware.schema)

  private[avro4s] def extractMapValueSchema[T](schemaFor: SchemaForV2[Map[String, T]]): SchemaForV2[T] = {
    require(schemaFor.schema.getType == Schema.Type.MAP,
            s"Schema type for map encoders / decoders must be MAP, received ${schemaFor.schema.getType}")
    SchemaForV2(schemaFor.schema.getValueType, schemaFor.fieldMapper)
  }

  @inline
  private[avro4s] def encodeMap[T](encoder: EncoderV2[T], value: Map[String, T]): AnyRef = {
    val map = new util.HashMap[String, AnyRef]
    value.foreach { case (k, v) => map.put(k, encoder.encode(v)) }
    map
  }

  @inline
  private[avro4s] def decodeMap[T](decoder: DecoderV2[T], value: Any): Map[String, T] = value match {
    case map: java.util.Map[_, _] => map.asScala.toMap.map { case (k, v) => k.toString -> decoder.decode(v) }
  }
}