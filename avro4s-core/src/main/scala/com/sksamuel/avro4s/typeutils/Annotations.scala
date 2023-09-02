package com.sksamuel.avro4s.typeutils

import com.sksamuel.avro4s.{AvroAliasable, AvroDoc, AvroDocumentable, AvroEnumDefault, AvroErasedName, AvroError, AvroFixed, AvroName, AvroNameable, AvroNamespace, AvroNoDefault, AvroProp, AvroProperty, AvroSortPriority, AvroTransient, AvroUnionPosition}
import magnolia1.{CaseClass, TypeInfo}

class Annotations(annos: Seq[Any]) {

  def name: Option[String] = annos.collectFirst {
    case t: AvroNameable => t.name
  }

  def namespace: Option[String] = annos.collectFirst {
    case t: AvroNamespace => t.namespace
  }

  def aliases: Seq[String] = annos.collect {
    case t: AvroAliasable => t.alias
  }.filterNot(_.trim.isEmpty)

  def props: Map[String, String] = annos.collect {
    case t: AvroProperty => (t.key, t.value)
  }.toMap

  def doc: Option[String] = annos.collectFirst {
    case t: AvroDocumentable => t.doc
  }

  def transient: Boolean = annos.collectFirst {
    case t: AvroTransient => t
  }.isDefined
  
  def nodefault: Boolean = annos.collectFirst {
    case t: AvroNoDefault => t
  }.isDefined

  def erased: Boolean = annos.collectFirst {
    case t: AvroErasedName => t
  }.isDefined

  def error: Boolean = annos.collectFirst {
    case t: AvroError => t
  }.isDefined

  /**
    * Returns the fixed size when a type or field is annotated with @AvroFixed
    */
  def fixed: Option[Int] = annos.collectFirst {
    case t: AvroFixed => t.size
  }

  private def avroSortPriority: Option[Float] = annos.collectFirst {
    case t: AvroSortPriority => t.priority
  }

  private def avroUnionPosition: Option[Float] = annos.collectFirst {
    case t: AvroUnionPosition => 999999f - t.position
  }

  def sortPriority: Option[Float] = avroSortPriority.orElse(avroUnionPosition)

  def enumDefault: Option[Any] = annos.collectFirst {
    case t: AvroEnumDefault => t.default
  }

}

object Annotations {
  def apply(ctx: CaseClass[_, _]): Annotations = new Annotations(ctx.annotations)
  def apply(annos: Seq[Any]): Annotations = new Annotations(annos)
}