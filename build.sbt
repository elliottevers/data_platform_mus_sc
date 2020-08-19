import com.lightbend.lagom.core.LagomVersion

organization in ThisBuild := "com.tk_music"
version in ThisBuild := "1.0-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.13.0"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.3" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1" % Test
val postgresDriver = "org.postgresql" % "postgresql" % "42.2.8"

lagomKafkaPropertiesFile in ThisBuild :=
  Some((baseDirectory in ThisBuild).value / "project" / "kafka-server.properties")

lazy val `platform` = (project in file("."))
  .aggregate(`label-api`, `label-impl`, `sample-api`, `sample-impl`, `data-api`, `data-impl`)

lazy val `label-api` = (project in file("label-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `label-impl` = (project in file("label-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslPersistenceJdbc,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      scalaTest,
      postgresDriver,
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`label-api`)

lazy val `sample-api` = (project in file("sample-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  ).dependsOn(`data-api`)

lazy val `sample-impl` = (project in file("sample-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslKafkaClient,
      lagomScaladslTestKit,
      macwire,
      scalaTest,
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`sample-api`)
  .dependsOn(`data-api`)


lazy val `data-api` = (project in file("data-api"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `data-impl` = (project in file("data-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslPersistenceJdbc,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      scalaTest,
      postgresDriver
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`data-api`)

lagomCassandraEnabled in ThisBuild := false
lagomCassandraPort in ThisBuild := 9042

// Use Kafka server running in a docker container
lagomKafkaEnabled in ThisBuild := false
lagomKafkaPort in ThisBuild := 9092
