package com.blstream.jess

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.http.scaladsl._
import scala.concurrent.duration._

object Main extends App with HttpServiceActor {

  implicit val system = ActorSystem("jess")
  implicit val flowMaterializer = ActorMaterializer()

  implicit val timeout = Timeout(5.seconds)

  import scala.concurrent.ExecutionContext.Implicits.global

  val binding = Http().bindAndHandle(route, "0.0.0.0", 8090)

}
