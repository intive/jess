package com.blstream.jess

import akka.actor.{ ActorSystem, Props }
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import scala.concurrent.duration._

object Main extends App {

  implicit val system = ActorSystem("jess")
  val service = system.actorOf(Props[HttpService], "jess-http-service")
  implicit val timeout = Timeout(5.seconds)

  IO(Http) ? Http.Bind(service, interface = "localhost", port = 8090)

}
