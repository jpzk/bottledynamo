
lazy val commonSettings = Seq(
  organization := "com.madewithtea",
  version := "1.0.0",
  scalaVersion := "2.12.1",
  crossScalaVersions := Seq("2.12.2","2.11.11"),
  description := "Good enough DynamoDB abstraction in Scala with Circe JSON serialization",
  organizationHomepage := Some(url("https://www.madewithtea.com")))

val scalaTestVersion = "3.0.2"
val circeVersion = "0.7.0"
val twitterVersion = "6.40.0"
val dynamoDbVersion = "1.11.77"

lazy val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
lazy val ddb = "com.amazonaws" % "aws-java-sdk-dynamodb" % dynamoDbVersion 
lazy val circe = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)
lazy val twitterUtilCore = "com.twitter" %% "util-core" % twitterVersion

lazy val bottledynamo = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= Seq(
      ddb,
      twitterUtilCore,
      scalaTest
    ) ++ circe
  )

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>https://github.com/jpzk/bottledynamo</url>
    <licenses>
      <license>
        <name>Apache License Version 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:jpzk/bottledynamo.git</url>
      <connection>scm:git:git@github.com:jpzk/bottledynamo.git</connection>
    </scm>
    <developers>
      <developer>
        <id>jpzk</id>
        <name>Jendrik Poloczek</name>
        <url>https://www.madewithtea.com</url>
      </developer>
    </developers>
  )

