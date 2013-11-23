name := "the-algorithm"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "com.typesafe.akka" %% "akka-channels-experimental" % "2.2.3",
  "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
  "play-json-zipper" %% "play-json-zipper" % "1.0"
)     

play.Project.playScalaSettings
