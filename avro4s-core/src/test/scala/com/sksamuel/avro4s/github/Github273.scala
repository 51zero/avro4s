package com.sksamuel.avro4s.github

import com.sksamuel.avro4s.{DefaultFieldMapper, SchemaFor}
import shapeless.{:+:, CNil}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

case class Coproducts(cp: Int :+: String :+: Boolean :+: CNil)
case class CoproductOfCoproductsField(cp: Coproducts :+: Boolean :+: CNil)

class Github273 extends AnyFunSuite with Matchers {

  test("Diverging implicit expansion for SchemaFor in Coproducts inside case classes #273") {
    SchemaFor[CoproductOfCoproductsField].schema(DefaultFieldMapper)
  }
}
