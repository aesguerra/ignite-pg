package org.minyodev.spark

import org.apache.ignite.spark.{IgniteContext, IgniteRDD}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}

/**
  * This is basically copied from https://github.com/apache/ignite/blob/master/examples/src/main/scala/org/apache/ignite/scalar/examples/spark/ScalarSharedRDDExample.scala
  * Just to run "Hello World" with Apache Ignite
  * */
object SharedRDDWithXmlConfig extends App {

  // Spark Configuration.
  private val conf = new SparkConf()
    .setAppName(this.getClass.getName)
    .setMaster("local")
    .set("spark.executor.instances", "2")

  // Spark context.
  val sparkContext = new SparkContext(conf)

  // Adjust the logger to exclude the logs of no interest.
  Logger.getRootLogger.setLevel(Level.ERROR)
  Logger.getLogger("org.apache.ignite").setLevel(Level.INFO)

  // Defines spring cache Configuration path.
  private val CONFIG = "src/main/resources/example-shared-rdd.xml"

  // Creates Ignite context with above configuration.
  val igniteContext = new IgniteContext(sparkContext, CONFIG, false)

  // Creates an Ignite Shared RDD of Type (Int,Int) Integer Pair.
  val sharedRDD: IgniteRDD[Int, Int] = igniteContext.fromCache[Int, Int]("sharedRDD")

  // Fill the Ignite Shared RDD in with Int pairs.
  sharedRDD.savePairs(sparkContext.parallelize(1 to 100000, 10).map(i => (i, i)))

  // Transforming Pairs to contain their Squared value.
  sharedRDD.mapValues(x => (x * x))

  // Retrieve sharedRDD back from the Cache.
  val transformedValues: IgniteRDD[Int, Int] = igniteContext.fromCache("sharedRDD")

  // Perform some transformations on IgniteRDD and print.
  val squareAndRootPair = transformedValues.map { case (x, y) => (x, Math.sqrt(y.toDouble)) }

  println(">>> Transforming values stored in Ignite Shared RDD...")

  // Filter out pairs which square roots are less than 100 and
  // take the first five elements from the transformed IgniteRDD and print them.
  squareAndRootPair.filter(_._2 < 100.0).take(5).foreach(println)

  println(">>> Executing SQL query over Ignite Shared RDD...")

  // Execute a SQL query over the Ignite Shared RDD.
  val df = transformedValues.sql("select _val from Integer where _val < 100 and _val > 9 ")

  // Show ten rows from the result set.
  df.show(10)

  // Close IgniteContext on all workers.
  igniteContext.close(true)

  // Stop SparkContext.
  sparkContext.stop()
}
