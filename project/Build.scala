import sbt.Keys._
import sbt._

/** Adds common settings automatically to all subprojects */
object Build extends AutoPlugin {

  object autoImport {
    val org = "com.sksamuel.avro4s"
    val AvroVersion = "1.11.0"
    val Log4jVersion = "1.2.17"
    val ScalatestVersion = "3.2.16"
    val Slf4jVersion = "2.0.7"
    val Json4sVersion = "4.0.6"
    val CatsVersion = "2.7.0"
    val RefinedVersion = "0.9.26"
    val ShapelessVersion = "2.3.7"
    val MagnoliaVersion = "1.3.3"
    val SbtJmhVersion = "0.3.7"
    val JmhVersion = "1.32"
  }

  import autoImport._

  def isGithubActions: Boolean = sys.env.getOrElse("CI", "false") == "true"
  def releaseVersion: String = sys.env.getOrElse("RELEASE_VERSION", "")
  def isRelease: Boolean = releaseVersion != ""
  def githubRunNumber: String = sys.env.getOrElse("GITHUB_RUN_NUMBER", "local")
  def ossrhUsername: String = sys.env.getOrElse("OSSRH_USERNAME", "")
  def ossrhPassword: String = sys.env.getOrElse("OSSRH_PASSWORD", "")
  def publishVersion: String = if (isRelease) releaseVersion else "5.0.0." + githubRunNumber + "-SNAPSHOT"

  override def trigger = allRequirements
  override def projectSettings = publishingSettings ++ Seq(
    organization := org,
    scalaVersion := "3.3.0",
    resolvers += Resolver.mavenLocal,
    Test / parallelExecution := false,
    Test / scalacOptions ++= Seq("-Xmax-inlines:64"),
    javacOptions := Seq("-source", "1.8", "-target", "1.8"),    
    libraryDependencies ++= Seq(
      "org.scala-lang"    % "scala3-compiler_3" % scalaVersion.value,
      "org.apache.avro"   % "avro"              % AvroVersion,
      "org.slf4j"         % "slf4j-api"         % Slf4jVersion          % "test",
      "log4j"             % "log4j"             % Log4jVersion          % "test",
      "org.slf4j"         % "log4j-over-slf4j"  % Slf4jVersion          % "test",
      "org.scalatest"     % "scalatest_3"       % ScalatestVersion      % "test"
    )
  )

  val publishingSettings = Seq(
    publishMavenStyle := true,
    Test / publishArtifact := false,
    credentials += Credentials(
      "Sonatype Nexus Repository Manager",
      "oss.sonatype.org",
      ossrhUsername,
      ossrhPassword
    ),
    version := publishVersion,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isRelease) {
        Some("releases" at s"${nexus}service/local/staging/deploy/maven2")
      } else {
        Some("snapshots" at s"${nexus}content/repositories/snapshots")
      }
    },
    pomExtra := {
      <url>https://github.com/sksamuel/avro4s</url>
        <licenses>
          <license>
            <name>The Apache 2.0 License</name>
            <url>https://opensource.org/licenses/Apache-2.0</url>
            <distribution>repo</distribution>
          </license>
        </licenses>
        <scm>
          <url>git@github.com:sksamuel/avro4s.git</url>
          <connection>scm:git@github.com:sksamuel/avro4s.git</connection>
        </scm>
        <developers>
          <developer>
            <id>sksamuel</id>
            <name>sksamuel</name>
            <url>http://github.com/sksamuel</url>
          </developer>
        </developers>
    }
  )
}
