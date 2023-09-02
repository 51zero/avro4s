package com.sksamuel.avro4s.schemas

import com.sksamuel.avro4s.{AvroName, SchemaFor}
import com.sksamuel.avro4s.typeutils.{Annotations, Names, SubtypeOrdering}
import magnolia1.SealedTrait
import org.apache.avro.{Schema, SchemaBuilder}
import com.sksamuel.avro4s.CustomDefaults

object SealedTraits {
  def schema[T](ctx: SealedTrait[SchemaFor, T]): Schema = {

    val names = Names(ctx.typeInfo, Annotations(ctx.annotations))

    // It currently appears (magnolia1, v 1) that all the enum elements carry the same annotation set
    // as the enumeration symbol itself, which leads to name clash in the generated schema. 
    // For now I'm disabling AvroName annotation interpretation of the symbols
    // if its name equals to the whole enumeration name.
    // Annotaions that are attached to the enum elements are not visible here.
    // Looks lilke we ned to have a look into either Magnolia or Scala 3.
    val symbols = ctx.subtypes.sorted(SubtypeOrdering).map { st =>
      Names(
        st.typeInfo,
        Annotations(
          st.annotations.filter {
            case an: AvroName => an.name != names.name
            case _ => true
          }
        )
      ).name
    }

    val builder = SchemaBuilder.enumeration(names.name).namespace(names.namespace)

    val builderWithDefault = CustomDefaults.sealedTraitEnumDefaultValue(ctx) match {
      case Some(default) => 
        builder.defaultSymbol(default)
      case None          => builder
    }

    builderWithDefault.symbols(symbols*)
    
  }
}