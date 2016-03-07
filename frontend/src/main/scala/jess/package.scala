package com.blstream

package object jess {
  import scala.scalajs.js
  import js.Dynamic.global
  import js.Any

  def consoleLog(any: js.Any) = global.console.log(any)
}
