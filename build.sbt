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
  .aggregate(`partition-api`, `partition-impl`, `sample-api`, `sample-impl`, `data-api`, `data-impl`)

lazy val `partition-api` = (project in file("partition-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )
//  .dependsOn(`partition-api`)
  .dependsOn(`data-api`)

lazy val `partition-impl` = (project in file("partition-impl"))
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
  .dependsOn(`partition-api`)
  .dependsOn(`data-api`)
  .dependsOn(`data-impl`)

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
      lagomScaladslPersistenceCassandra,
      lagomScaladslPersistenceJdbc,
      macwire,
      scalaTest,
      "com.softwaremill.sttp.client" %% "core" % "2.2.9",
      "com.softwaremill.sttp.client" %% "http4s-backend" % "2.2.9",
      "org.typelevel" %% "cats-effect" % "2.2.0"
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`sample-api`)
  .dependsOn(`partition-api`)
  .dependsOn(`partition-impl`)
  .dependsOn(`data-api`)
  .dependsOn(`data-impl`)


lazy val `data-api` = (project in file("data-api"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      "com.beachape" %% "enumeratum" % "1.6.1",
      "com.beachape" %% "enumeratum-play-json" % "1.6.1"
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
      postgresDriver,
      "com.beachape" %% "enumeratum" % "1.6.1",
      "com.beachape" %% "enumeratum-play-json" % "1.6.1",
      "com.github.pureconfig" %% "pureconfig" % "0.14.0",
      "com.softwaremill.sttp.client" %% "core" % "2.2.9",
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % "3.0.0-RC6",
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`data-api`)

lagomCassandraEnabled in ThisBuild := false
lagomCassandraPort in ThisBuild := 9042

// Use Kafka server running in a docker container
lagomKafkaEnabled in ThisBuild := false
lagomKafkaPort in ThisBuild := 9092
