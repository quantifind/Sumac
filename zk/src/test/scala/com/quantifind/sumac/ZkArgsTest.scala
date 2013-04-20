package com.quantifind.sumac

import org.scalatest.{Tag, FunSuite}
import com.twitter.conversions.time._
import org.scalatest.matchers.ShouldMatchers
import com.twitter.util.JavaTimer


class ZkArgsTest extends FunSuite with ShouldMatchers {

  val runZkTests = true
  val zkTestHost = "localhost:2181"
  def zkTest(testName: String, tags: Tag*)(testFun: => Unit) =
    if (runZkTests)
      test(testName, tags :_*)(testFun)
    else
      ignore(testName, tags :_*)(testFun) //by default, don't run these tests, since they need a local zookeeper running.

  zkTest("readFromZk") {
    implicit val timer = new JavaTimer(false)

    val testPath = "/blammo"
    val zk = ZkArgHelper.basicZkClient(zkTestHost, 5.seconds)
    val n = zk(testPath)
    if (ZkArgHelper.nodeExists(n))
      ZkArgHelper.deleteRecursively(n)

    val initArgs = ZkArgHelper.getArgsFromZk(zk, testPath)
    initArgs should be ('empty)
    val argsToWrite = Map("x" -> "17", "ooga" -> "      hi there")
    ZkArgHelper.saveArgsToZk(zk, testPath, argsToWrite)
    val readArgs = ZkArgHelper.getArgsFromZk(zk, testPath)
    readArgs should equal (argsToWrite)

    val secondArgsToWrite = Map("x" -> "19", "blah" -> "wakka")
    ZkArgHelper.saveArgsToZk(zk, testPath, secondArgsToWrite)
    val secondReadArgs = ZkArgHelper.getArgsFromZk(zk, testPath)
    secondReadArgs should equal (secondArgsToWrite)
  }

}
