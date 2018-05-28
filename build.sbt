name := "ignite-pg"

version := "0.1"

scalaVersion := "2.11.8"

val sparkVersion = "2.1.0"
val igniteVersion = "2.3.0"

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "3.6.0",
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-hive" % sparkVersion % "provided",
  "org.apache.curator" % "curator-framework" % "2.9.1",
  "org.apache.ignite" % "ignite-core" % igniteVersion,
  ("org.apache.ignite" % "ignite-spark" % igniteVersion)
    .exclude("org.apache.spark", "spark-tags_2.10")
    .exclude("org.apache.spark", "spark-unsafe_2.10")
    .exclude("org.scalatest", "scalatest_2.10")
    .exclude("com.twitter", "chill_2.10"),
  "org.apache.ignite" % "ignite-zookeeper" % igniteVersion,
  "org.apache.ignite" % "ignite-log4j" % igniteVersion,
  "org.apache.ignite" % "ignite-yarn" % igniteVersion
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
