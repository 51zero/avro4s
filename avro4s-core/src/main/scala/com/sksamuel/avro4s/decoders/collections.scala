package com.sksamuel.avro4s.decoders

import com.sksamuel.avro4s.{Avro4sDecodingException, Decoder}
import org.apache.avro.Schema
import scala.jdk.CollectionConverters._

import scala.reflect.ClassTag
import scala.jdk.CollectionConverters._

class ArrayDecoder[T: ClassTag](decoder: Decoder[T]) extends Decoder[Array[T]] :
  def decode(schema: Schema): Any => Array[T] = {
    require(schema.getType == Schema.Type.ARRAY, {
      s"Require schema type ARRAY (was $schema)"
    })
    val decodeT = decoder.decode(schema.getElementType)
    { value =>
      value match {
        case array: Array[_] => array.map(decodeT)
        case list: java.util.Collection[_] => list.asScala.map(decodeT).toArray
        case list: Iterable[_] => list.map(decodeT).toArray
        case other => throw new Avro4sDecodingException("Unsupported array " + other, value)
      }
    }
  }

trait CollectionDecoders:
  given[T: ClassTag](using decoder: Decoder[T]): Decoder[Array[T]] = ArrayDecoder[T](decoder)
  given[T](using decoder: Decoder[T]): Decoder[List[T]] = iterableDecoder(decoder, _.toList)
  given[T](using decoder: Decoder[T]): Decoder[Seq[T]] = iterableDecoder(decoder, _.toSeq)
  given[T](using decoder: Decoder[T]): Decoder[Set[T]] = iterableDecoder(decoder, _.toSet)
  given[T](using decoder: Decoder[T]): Decoder[Vector[T]] = iterableDecoder(decoder, _.toVector)
  given mapDecoder[T](using decoder: Decoder[T]): Decoder[Map[String, T]] = new MapDecoder[T](decoder)

  def iterableDecoder[T, C[X] <: Iterable[X]](decoder: Decoder[T],
                                              build: Iterable[T] => C[T]): Decoder[C[T]] =
    new Decoder[C[T]] {
      def decode(schema: Schema): Any => C[T] = {
        require(schema.getType == Schema.Type.ARRAY, {
          s"Require schema type ARRAY (was $schema)"
        })
        val decodeT = decoder.decode(schema.getElementType)
        { value =>
          value match {
            case list: java.util.Collection[_] => build(list.asScala.map(decodeT))
            case list: Iterable[_] => build(list.map(decodeT))
            case array: Array[_] =>
              // converting array to Seq in order to avoid requiring ClassTag[T] as does arrayDecoder.
              build(array.toSeq.map(decodeT))
            case other => throw new Avro4sDecodingException("Unsupported collection type " + other, value)
          }
        }
      }
    }

class MapDecoder[T](decoder: Decoder[T]) extends Decoder[Map[String, T]] :
  override def decode(schema: Schema): Any => Map[String, T] = {
    require(schema.getType == Schema.Type.MAP)
    val decode = decoder.decode(schema.getValueType)
    { value =>
      value match {
        case map: java.util.Map[_, _] => map.asScala.toMap.map { case (k, v) => k.toString -> decode(v) }
      }
    }
  }