package com.sksamuel.avro4s.refined

import com.sksamuel.avro4s.streams.input.InputStreamTest
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString
import shapeless.*

import scala.util.Failure

class RefinedRoundtripTest extends InputStreamTest:

  type C1 = NonEmptyString :+: CNil
  case class Container1(c1: C1)
//  type C2 = Int :+: NonEmptyString :+: CNil
//  case class Container2(c2: C2)
//  type C3 = PosInt :+: NonEmptyString :+: CNil
//  case class Container3(c3: C3)
//  case class Container4(map: Map[String, NonEmptyString], c3: C3, list: List[(Int, PosInt)])
//  type C1b = String :+: CNil
//  case class Container1b(c1: C1b)
//  case class Container5(c5: Either[NonEmptyString, Int])
//  case class Container6(c6: Map[NonEmptyString, PosInt])

//  test("a union of one refined type inside a record should rountrip"):
//    writeRead(Container1(Coproduct[C1](NonEmptyString.unsafeFrom("a"))))

//  test("a union of one refined type and more standard types inside a record should rountrip") {
//    writeRead(Container2(Coproduct[C2](NonEmptyString("a"))))
//  }

//  test("a union of more than one refined type inside a record should rountrip") {
//    writeRead(Container3(Coproduct[C3](PosInt(42))))
//  }

//  test("a more complex record should rountrip") {
//    writeRead(Container4(Map("bla" -> NonEmptyString("a")), Coproduct[C3](NonEmptyString("b")), List(23 -> PosInt(42), 42 -> PosInt(23))))
//  }

//  test("a broken encoder will not decode") {
//    val out = writeData(Container1b(Coproduct[C1b]("")))
//    val result = tryReadData[Container1](out.toByteArray).next()
//    result should matchPattern { case Failure(iae: IllegalArgumentException) if iae.getMessage == "Predicate isEmpty() did not fail." => }
//  }

//  test("an either of one refined type inside a record should roundtrip") {
//    writeRead(Container5(Left(NonEmptyString("a"))))
//  }

//  test("a map with refined types on both key and value should roundtrip") {
//    val key: NonEmptyString = NonEmptyString("foo")
//    val value: PosInt = PosInt(1)
//    writeRead(Container6(Map(key -> value)))
//  }
