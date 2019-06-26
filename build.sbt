// Note: settings common to all subprojects are defined in project/GlobalPlugin.scala

// The root project is implicit, so we don't have to define it.
// We do need to prevent publishing for it, though:

lazy val root = Project("avro4s", file("."))
  .settings(
    publish := {},
    publishArtifact := false,
    name := "avro4s"
  )
  .aggregate(
    `avro4s-core`,
    `avro4s-json`,
   // `avro4s-cats`,
    `avro4s-kafka`
  )

val `avro4s-core` = project.in(file("avro4s-core"))
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill" %% "magnolia" % "0.11.0-sml",
      "com.chuusai" %% "shapeless" % ShapelessVersion
    )
  )

val `avro4s-json` = project.in(file("avro4s-json"))
  .dependsOn(`avro4s-core`)
  .settings(
    libraryDependencies ++= Seq(
      "org.json4s" %% "json4s-native" % Json4sVersion
    )
  )

//val `avro4s-cats` = project.in(file("avro4s-cats"))
//  .dependsOn(`avro4s-core`)
//  .settings(
//    libraryDependencies ++= Seq(
//      "org.typelevel" %% "cats-core" % CatsVersion
//    )
//  )

val `avro4s-kafka` = project.in(file("avro4s-kafka"))
  .dependsOn(`avro4s-core`)
  .settings(
    libraryDependencies ++= Seq(
      "org.apache.kafka" % "kafka-clients" % "2.3.0"
    )
  )
