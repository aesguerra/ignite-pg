package org.minyodev.spark

import scala.util.Random

object Play extends App {
  (0 to 100).foreach(x => println(Random.nextInt(4)))
}
