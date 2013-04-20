package com.quantifind.sumac

import org.scalatest.{Tag, FunSuite}
import com.twitter.conversions.time._
import org.scalatest.matchers.ShouldMatchers
import com.twitter.util.JavaTimer
import com.quantifind.sumac


class ZkArgsTest extends FunSuite with ShouldMatchers {

  val runZkTests = true
  val zkTestHost = "localhost:2181"
  val zkRootPath = "/ZkArgsTest"

  if (runZkTests) {
    implicit val timer = new JavaTimer(false)
    val zk = ZkArgHelper.basicZkClient(zkTestHost, 5.seconds)
    val root = zk(zkRootPath)
    if (ZkArgHelper.nodeExists(root))
      ZkArgHelper.deleteRecursively(root)
    root.create().apply()
  }


  def zkTest(testName: String, tags: Tag*)(testFun: => Unit) =
    if (runZkTests)
      test(testName, tags :_*)(testFun)
    else
      ignore(testName, tags :_*)(testFun) //by default, don't run these tests, since they need a local zookeeper running.

  zkTest("readFromZk") {
    implicit val timer = new JavaTimer(false)

    val testPath = zkRootPath + "/blammo"
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


  zkTest("args.saveToZk"){
    val args = new MyArgs()
    args.x = 17
    args.y = 0.9f
    args.zkConn = zkTestHost

    val path = zkRootPath + "/args.saveToZk"
    implicit val timer = args.timer
    args.saveToZk(path)

    ZkArgHelper.getArgsFromZk(args.zkClient, path) should be (
      //some of these need to be cleaned up
      Map("x" -> "17", "y" -> "0.9", "zkConn" -> zkTestHost, "ooga" -> "<null>", "zkPaths" -> "List()")
    )
  }

  zkTest("args.loadFromZk"){
    val argsInZk = Map("x" -> "87")
    implicit val timer = new JavaTimer(false)
    val testPath = zkRootPath + "/loadArgs1"
    val zk = ZkArgHelper.basicZkClient(zkTestHost, 5.seconds)
    ZkArgHelper.saveArgsToZk(zk, testPath, argsInZk)

    val args = new MyArgs()
    args.parse(Map("zkConn" -> zkTestHost, "zkPaths" -> testPath))
    args.x should be (87)
  }

}

class MyArgs extends FieldArgs with ZkArgs {
  var x: Int = _
  var y: Float = _
  var ooga: String = _
}
