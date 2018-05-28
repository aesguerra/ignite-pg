# ignite-spark examples

This project's purpose is to get more familiar with ignite-spark.

## Getting started

### Prerequisites

For the examples, we used the following versions.
1. Scala version
```2.11.8```
2. Apache Spark
```
val sparkVersion = "2.2.0"
libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-core" % sparkVersion,
    "org.apache.spark" %% "spark-sql" % sparkVersion,
    "org.apache.spark" %% "spark-hive" % sparkVersion
)
```
3. Apache Ignite
```
val igniteVersion = "2.4.0"
libraryDependencies ++= Seq(
    "org.apache.ignite" % "ignite-core" % igniteVersion,
    "org.apache.ignite" % "ignite-zookeeper" % igniteVersion,
    "org.apache.ignite" % "ignite-spark" % igniteVersion,
    "org.apache.ignite" % "ignite-log4j" % igniteVersion
)
```
4. Zookeeper version ```3.4.10```
