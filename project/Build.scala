import sbt.Keys._
import sbt._

/** Adds common settings automatically to all subprojects */
object Build extends AutoPlugin {

  object autoImport {
    val org = "com.sksamuel.avro4s"
    val AvroVersion = "1.11.4"
    val Log4jVersion = "1.2.17"
    val ScalatestVersion = "3.2.17"
    val Slf4jVersion = "2.0.16"
    val Json4sVersion = "3.6.11"
    val CatsVersion = "2.13.0"
    val RefinedVersion = "0.9.26"
    val ShapelessVersion = "2.3.7"
    val MagnoliaVersion = "1.1.4"
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
    scalaVersion := "3.3.4",
    resolvers += Resolver.mavenLocal,
    Test / parallelExecution := false,
    Test / scalacOptions ++= Seq("-Xmax-inlines:100", "-Yretain-trees"),
    javacOptions := Seq("-source", "1.8", "-target", "1.8"),    
    libraryDependencies ++= Seq(
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
      "s01.oss.sonatype.org",
      ossrhUsername,
      ossrhPassword
    ),
    version := publishVersion,
    publishTo := {
      if (isRelease) {
        Some("releases" at "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
      } else {
        Some("snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots/")
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

