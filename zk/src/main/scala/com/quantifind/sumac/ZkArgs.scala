package com.quantifind.sumac

import com.twitter.zk.{ZNode, ZkClient}
import com.twitter.conversions.time._
import com.twitter.util.{Duration, Timer, JavaTimer}
import collection.JavaConverters._
import collection._

/**
 * Add this into args to be able to read args from zookeeper.
 *
 * Note that it does *NOT* write the arguments back to zookeeper automatically.  This is because in general, zookeeper
 * would just be used to store some defaults, which you don't want to get overridden all the time.
 */
trait ZkArgs extends ExternalConfig {
  self: Args =>

  var zkConn: String = _
  var zkPaths: List[String] = List()

  val timeout = 5.seconds

  implicit lazy val timer = new JavaTimer(false)
  lazy val zkClient = ZkArgHelper.basicZkClient(zkConn, timeout)
  def saveToZk(zkPath: String) = {
    ZkArgHelper.saveArgsToZk(zkClient, zkPath, getStringValues)
  }

  abstract override def readArgs(originalArgs: Map[String,String]): Map[String,String] = {
    parse(originalArgs, false)
    val newArgs = zkPaths.foldLeft(originalArgs){case(prev, nextPath) =>
      val zkArgs = ZkArgHelper.getArgsFromZk(zkClient, nextPath)
      ExternalConfigUtil.mapWithDefaults(prev, zkArgs)
    }
    super.readArgs(newArgs)
  }

  abstract override def saveConfig() {
    //we intentionally do *NOT* save back to zookeeper
    super.saveConfig()
  }

}

object ZkArgHelper {

  def basicZkClient(zkConn: String, timeout: Duration)(implicit timer: Timer) = ZkClient(zkConn, timeout).withAcl(org.apache.zookeeper.ZooDefs.Ids.OPEN_ACL_UNSAFE.asScala)

  def getArgsFromZk(zkClient: ZkClient, path: String)(implicit timer: Timer): Map[String,String] = {
    val n = zkClient(path)
    if (nodeExists(n)) {
      val childrenOp = zkClient.apply(path).getChildren
      val children = childrenOp.apply().apply()
      val childToData = children.children.map{child =>
        child.name -> new String(child.getData.apply().apply().bytes)
      }.toMap
      zkClient.release().apply()
      childToData
    } else {
      Map()
    }
  }

  def saveArgsToZk(zkClient: ZkClient, path: String, args: Map[String,String])(implicit timer: Timer) {
    implicit val timer = new JavaTimer(false)
    val node = zkClient.apply(path)
    //first, remove this node, to clear any properties previously set
    if (nodeExists(node))
      deleteRecursively(node)

    //now recreate the node, and its children w/ all the values
    node.create().apply()
    args.foreach{case(k,v) =>
      node.create(child=Some(k), data = v.getBytes).apply()
    }
  }

  def nodeExists(node: ZNode): Boolean = {
    //there has got to be a better way ...
    try {
      node.exists.apply().apply().stat != null
    } catch {
      case ex: Exception => false
    }
  }

  def deleteRecursively(node: ZNode) {
    node.getChildren()().children.foreach{child =>
      deleteRecursively(child)
    }
    node.delete()()
  }

}
