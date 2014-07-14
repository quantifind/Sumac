package com.quantifind.sumac.scalatra

import com.quantifind.sumac.Args
import org.scalatra.servlet.ServletBase
import javax.servlet.http.HttpServletRequest

trait ScalatraArgs {
  self: Args =>

  def parse(servlet: ServletBase)(implicit request: HttpServletRequest) {
    ScalatraArgs.parse(this, servlet)
  }
}

object ScalatraArgs {
  def parse(args: Args, servlet: ServletBase)(implicit request: HttpServletRequest) {
    args.parse(servlet.multiParams.map{case(k,v) => k -> (v.mkString(","))})
  }
}
