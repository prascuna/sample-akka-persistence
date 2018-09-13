import sbt._

object Dependencies {
  private val akkaVersion = "2.5.15"

  lazy val test = Seq(
    "org.scalatest" %% "scalatest" % "3.0.5" % "test"
  )

  lazy val compile = Seq(
    "org.slf4j" % "slf4j-simple" % "1.8.0-beta2",
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-remote" % akkaVersion,
    "com.typesafe.akka" %% "akka-http"   % "10.1.4",
    "com.typesafe.akka" %% "akka-persistence-dynamodb" % "1.1.1",
//    "com.lightbend.akka" %% "akka-stream-alpakka-kinesis" % "0.20",
    "software.amazon.kinesis" % "amazon-kinesis-client" % "2.0.2",
    "com.amazonaws" % "amazon-kinesis-producer" % "0.12.9",
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
  )
}
