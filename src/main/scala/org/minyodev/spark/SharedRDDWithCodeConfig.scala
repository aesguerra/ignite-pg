package org.minyodev.spark

import org.apache.ignite.cache.{CacheAtomicityMode, CacheMode}
import org.apache.ignite.configuration.{CacheConfiguration, IgniteConfiguration}
import org.apache.ignite.spark.{IgniteContext, IgniteRDD}
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession

/**
  * An example where we define cache properties in code, not using spring cache configuration file
  * */
object SharedRDDWithCodeConfig extends App {

  val spark: SparkSession = SparkSession.builder()
    .appName(this.getClass.getName)
    .master("local[*]")
    .getOrCreate()

  Logger.getRootLogger.setLevel(Level.ERROR)
  Logger.getLogger("org.apache.ignite").setLevel(Level.INFO)

  // Defines cache Configuration
  val cacheConfig = new CacheConfiguration()
    .setName("sharedRDD")
    .setCacheMode(CacheMode.PARTITIONED)
    .setIndexedTypes(classOf[java.lang.Integer], classOf[java.lang.Integer])
    .setAtomicityMode(CacheAtomicityMode.ATOMIC)
    .setBackups(1)

  val addresses = new java.util.ArrayList[String]()
  addresses.add("127.0.0.1:47500")

  val ipFinder = new TcpDiscoveryMulticastIpFinder()
    .setAddresses(addresses)

  val discoverySpi = new TcpDiscoverySpi()
    .setIpFinder(ipFinder)

  val igc = new IgniteConfiguration()
    .setCacheConfiguration(cacheConfig)
    .setDiscoverySpi(discoverySpi)

  // Creates Ignite context with above configuration.
  val igniteContext = new IgniteContext(spark.sparkContext, () => igc, false)

  // Creates an Ignite Shared RDD of Type (Int,Int) Integer Pair.
  val sharedRDD: IgniteRDD[Int, Int] = igniteContext.fromCache[Int, Int]("sharedRDD")

  // Fill the Ignite Shared RDD in with Int pairs.
  sharedRDD.savePairs(spark.sparkContext.parallelize(1 to 100000, 10).map(i => (i, i)))

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
}
