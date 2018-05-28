package org.minyodev.spark

import org.apache.ignite.cache.{CacheAtomicityMode, CacheMode}
import org.apache.ignite.configuration.{CacheConfiguration, IgniteConfiguration}
import org.apache.ignite.spark.{IgniteContext, IgniteRDD}
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi
import org.apache.ignite.spi.discovery.tcp.ipfinder.zk.TcpDiscoveryZookeeperIpFinder
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import scopt.OptionParser

object SharedRDDWithZookeeper extends App {

  case class JobOptions(igniteHome: String = "/Users/ts-ana.esguerra/local/personal/apache-ignite-2.3.0-src")

  val parser = new OptionParser[JobOptions](this.getClass.getName) {
    opt[String]("ignite-home")
      .optional()
      .action((arg, o) => o.copy(igniteHome = arg))
      .text("Ignite home")
  }

  val conf =
    parser.parse(args, JobOptions()) match {
      case None =>
        parser.showUsageAsError()
        throw new RuntimeException("Failed to parse args")
      case Some(c) => c
    }

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
    .setIndexedTypes(classOf[Int], classOf[Int])
    .setAtomicityMode(CacheAtomicityMode.ATOMIC)
    .setBackups(1)

  val ipFinder = new TcpDiscoveryZookeeperIpFinder()
    .setZkConnectionString("127.0.0.1:2181")

  val discoverySpi = new TcpDiscoverySpi()
    .setIpFinder(ipFinder)

  val igc = new IgniteConfiguration()
    .setCacheConfiguration(cacheConfig)
    .setDiscoverySpi(discoverySpi)

  val igniteContext = new IgniteContext(spark.sparkContext, () => igc.setIgniteHome(conf.igniteHome), false)
  val sharedRDD: IgniteRDD[Int, Int] = igniteContext.fromCache[Int, Int]("sharedRDD")
  sharedRDD.savePairs(spark.sparkContext.parallelize(1 to 100000, 10).map(i => (i, i)))
  sharedRDD.mapValues(x => (x * x))

  val transformedValues: IgniteRDD[Int, Int] = igniteContext.fromCache("sharedRDD")
  val squareAndRootPair = transformedValues.map { case (x, y) => (x, Math.sqrt(y.toDouble)) }

  println(">>> Transforming values stored in Ignite Shared RDD...")

  // Filter out pairs which square roots are less than 100 and
  // take the first five elements from the transformed IgniteRDD and print them.
  squareAndRootPair.filter(_._2 < 100.0).take(5).foreach(println)

  println(">>> Executing SQL query over Ignite Shared RDD...")

  // Execute a SQL query over the Ignite Shared RDD.
  val out = transformedValues.sql("select _val from Integer where _val < 100 and _val > 9 ")

  // Show ten rows from the result set.
  out.show(10)

  // Close IgniteContext on all workers.
  igniteContext.close(true)

}
