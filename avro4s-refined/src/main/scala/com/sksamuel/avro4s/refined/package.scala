package com.sksamuel.avro4s

import eu.timepit.refined.api.{RefType, Validate}

package object refined:

  given[T, P, F[_, _]](using schemaFor: SchemaFor[T]): SchemaFor[F[T, P]] = schemaFor.forType

  given[T: Encoder, P, F[_, _] : RefType]: Encoder[F[T, P]] = Encoder[T].contramap(RefType[F].unwrap)

  given[T: Decoder, P, F[_, _] : RefType](using validate: Validate[T, P]): Decoder[F[T, P]] = Decoder[T].map(RefType[F].refine[P].unsafeFrom[T])

  given[A, P, F[_, _]: RefType, B](using schemaForA: SchemaFor[A], schemaForB: SchemaFor[B], isString: A <:< String): SchemaFor[Map[F[A, P], B]] =
    SchemaFor.mapSchemaFor[B].forType

  given[A: Encoder, B: Encoder, P, F[_, _]: RefType](using isString: A <:< String): Encoder[Map[F[A, P], B]] =
    Encoder.mapEncoder[B].contramap[Map[F[A, P], B]]: theMap =>
      theMap.map:
        case (k, v) => RefType[F].unwrap(k).asInstanceOf[String] -> v

  given[A: Decoder, B: Decoder, P, F[_, _]: RefType](using validate: Validate[A, P], isString: A <:< String): Decoder[Map[F[A, P], B]] =
    Decoder.mapDecoder[B].map: theMap =>
      theMap.map:
        case (str, b) => (RefType[F].refine[P].unsafeFrom[A](str.asInstanceOf[A]), b)

//  implicit def refinedTypeGuardedDecoding[T: WeakTypeTag, P, F[_, _]: RefType]: TypeGuardedDecoding[F[T, P]] = new TypeGuardedDecoding[F[T, P]] {
//    override final def guard(decoderT: Decoder[F[T, P]]): PartialFunction[Any, F[T, P]] =
//      TypeGuardedDecoding[T].guard(decoderT.map(RefType[F].unwrap)).andThen(RefType[F].unsafeWrap(_))
//  }
