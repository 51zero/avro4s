// Note: settings common to all subprojects are defined in project/GlobalPlugin.scala

// The root project is implicit, so we don't have to define it.
// We do need to prevent publishing for it, though:
import sbt._

lazy val root = Project("avro4s", file("."))
  .settings(
    publish := {},
    publishArtifact := false,
    name := "avro4s"
  )
  .aggregate(
    `avro4s-core`,
    `avro4s-cats`,
//    `avro4s-kafka`
    `avro4s-refined`
  )

val `avro4s-core` = project.in(file("avro4s-core"))
  .settings(
    publishArtifact := true,
    libraryDependencies ++= Seq(
      "com.softwaremill.magnolia1_3" %% "magnolia" % MagnoliaVersion
      //      "com.chuusai" %% "shapeless" % ShapelessVersion,
      //      "org.json4s" %% "json4s-native" % Json4sVersion
    )
  )

val `avro4s-cats` = project.in(file("avro4s-cats"))
  .dependsOn(`avro4s-core`)
  .settings(
    publishArtifact := true,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % CatsVersion,
      "org.typelevel" %% "cats-jvm" % CatsVersion
    )
  )

//val `avro4s-kafka` = project.in(file("avro4s-kafka"))
//  .dependsOn(`avro4s-core`)
//  .settings(
//    libraryDependencies ++= Seq(
//      "org.apache.kafka" % "kafka-clients" % "2.4.0"
//    )
//  )

val `avro4s-refined` = project.in(file("avro4s-refined"))
  .dependsOn(`avro4s-core` % "compile->compile;test->test")
  .settings(
    libraryDependencies ++= Seq(
      "eu.timepit" %% "refined" % RefinedVersion
    )
  )

val benchmarks = project
  .in(file("benchmarks"))
  .dependsOn(`avro4s-core`)
  .enablePlugins(JmhPlugin)
  .settings(
    Test / fork := true,
    publishArtifact := false,
    libraryDependencies ++= Seq(
      "pl.project13.scala" % "sbt-jmh-extras" % SbtJmhVersion,
      "org.openjdk.jmh" % "jmh-core" % JmhVersion,
      "org.openjdk.jmh" % "jmh-generator-asm" % JmhVersion,
      "org.openjdk.jmh" % "jmh-generator-bytecode" % JmhVersion,
      "org.openjdk.jmh" % "jmh-generator-reflection" % JmhVersion
    )
  )