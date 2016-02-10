package com.blstream.jess

import akka.actor.ActorSystem
import akka.http.scaladsl._
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.duration._

object Main extends App with HttpServiceActor {

  implicit val system = ActorSystem("jess")
  implicit val flowMaterializer = ActorMaterializer()

  implicit val timeout = Timeout(5.seconds)

  import scala.concurrent.ExecutionContext.Implicits.global

  val binding = Http().bindAndHandle(route, "0.0.0.0", 8090)

}
