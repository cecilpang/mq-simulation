name := """mq-simulation"""

version := "1.0"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.2.3",
  "com.rabbitmq" % "amqp-client" % "3.2.2",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.10" % "2.3.1"
)
