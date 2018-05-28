package org.minyodev.spark

import org.apache.ignite.cache.{CacheAtomicityMode, CacheMode}
import org.apache.ignite.configuration.{CacheConfiguration, IgniteConfiguration}
import org.apache.ignite.spark.{IgniteContext, IgniteRDD}
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi
import org.apache.ignite.spi.discovery.tcp.ipfinder.zk.TcpDiscoveryZookeeperIpFinder
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.joda.time.format.DateTimeFormat
import scopt.OptionParser

import scala.util.Random

object SharedRDDSecondRun {

  case class JobOptions(zkString: String = null, outputPath: String = null)

  // Defines cache Configuration
  val cacheConfig = new CacheConfiguration()
    .setName("sharedRDD")
    .setCacheMode(CacheMode.PARTITIONED)
    .setIndexedTypes(classOf[java.lang.Integer], classOf[java.lang.Integer])
    .setAtomicityMode(CacheAtomicityMode.ATOMIC)
    .setBackups(1)

  val ipFinder = new TcpDiscoveryZookeeperIpFinder()
  val discoverySpi = new TcpDiscoverySpi()
  val igc = new IgniteConfiguration()

  def main(args: Array[String]): Unit = {

    val parser = new OptionParser[JobOptions](this.getClass.getName) {

      opt[String]("zkString")
        .required()
        .action((arg, o) => o.copy(zkString = arg))
        .text("Zookeeper String")

      opt[String]("output-path")
        .required()
        .action((arg, o) => o.copy(outputPath = arg))
        .text("output-path")
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
      .enableHiveSupport()
      .getOrCreate()

    Logger.getRootLogger.setLevel(Level.ERROR)
    Logger.getLogger("org.apache.ignite").setLevel(Level.INFO)

    ipFinder.setZkConnectionString(conf.zkString)
    discoverySpi.setIpFinder(ipFinder)
    igc
      .setCacheConfiguration(cacheConfig)
      .setDiscoverySpi(discoverySpi)

    val igniteContext = new IgniteContext(spark.sparkContext, () => igc, false)
    val transformedValues: IgniteRDD[Int, Int] = igniteContext.fromCache("sharedRDD")
    val reducedRdd = transformedValues.mapPartitions(itr => {
      val list = List("Foo", "Bar", "Mike", "Lulu")
      itr.map(x => {
        (list(Random.nextInt(4)), x._2)
      })
    })
      .reduceByKey(_ + _)
      .map(x => x._1 + "," + x._2)

    val fmt = DateTimeFormat.forPattern("yyyyMMddHHmmss")
    reducedRdd.saveAsTextFile(conf.outputPath + "/" + fmt.print(System.currentTimeMillis()))
  }
}
