name := "Alpakka-Sftp-Download-Java"
ThisBuild / scalaVersion := "2.13.8"
ThisBuild / scalacOptions += "-deprecation"

lazy val AkkaVersion = "2.6.17"
lazy val logbackVersion  = "1.2.3"
lazy val AlpakkaVersion = "3.0.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream-typed" % AkkaVersion,

  "com.lightbend.akka" %% "akka-stream-alpakka-ftp" % AlpakkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,

  "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,

  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test,
  "junit" % "junit" % "4.13.2" % Test
)