package com.quantifind.sumac

import com.twitter.zk.ZkClient
import com.twitter.conversions.time._
import com.twitter.util.{Timer, JavaTimer}
import collection.JavaConverters._

/**
 * Add this into args to be able to read & write args from zookeeper
 */
trait ZkArgs {
  var zkConn: String
  var zkPath: String

  val timeout = 5.seconds

  implicit val timer = new JavaTimer(false)
  private val zkClient = ZkClient(zkConn, timeout).withAcl(org.apache.zookeeper.ZooDefs.Ids.OPEN_ACL_UNSAFE.asScala)

}

object ZkArgHelper {
  def getArgsFromZk(zkClient: ZkClient, path: String)(implicit timer: Timer): Map[String,String] = {
    val childrenOp = zkClient.apply(path).getChildren
    val children = childrenOp.apply().apply()
    val childToData = children.children.map{child =>
      child.name -> new String(child.getData.apply().apply().bytes)
    }.toMap
    zkClient.release().apply()
    childToData
  }

  def saveArgsToZk(zkClient: ZkClient, path: String, args: Map[String,String])(implicit timer: Timer) {
    implicit val timer = new JavaTimer(false)
    val node = zkClient.apply(path)
    //first, remove this node, to clear any properties previously set
    node.delete().apply()

    //now recreate the node, and its children w/ all the values
    node.create().apply()
    args.foreach{case(k,v) =>
      node.create(child=Some(k), data = v.getBytes).apply()
    }
  }

}
