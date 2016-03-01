package com.blstream.jess
package api

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern._
import akka.util.Timeout
import cats.data.Xor
import cats.data.Xor.Left
import core.state.{ Challenge, SomeError }
import core.{ CorrectAnswer, GameActor, IncorrectAnswer, JessLink, ResponseAnswer, Stats }
import spray.json._

import concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

case class PostAnswerRequest(answer: String)

object PostAnswerRequest extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val format = new RootJsonReader[PostAnswerRequest] {
    def read(value: JsValue): PostAnswerRequest = {
      value.asJsObject.getFields("answer") match {
        case Seq(JsString(v)) => PostAnswerRequest(v)
        case _ => deserializationError(s"cannot deserialize request $value")
      }
    }

    def write(par: PostAnswerRequest): JsValue = JsString(par.answer)
  }
}

case class Meta(current: String, stats: String)

object Meta extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val format = jsonFormat2(Meta.apply)
}

object ChallengeFormat extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val formatChallenge: RootJsonFormat[Challenge] = jsonFormat3(Challenge)
}

case class ChallengeResponse(meta: Meta, challenge: Challenge)

object ChallengeResponse extends SprayJsonSupport with DefaultJsonProtocol {

  import ChallengeFormat._

  implicit val format = jsonFormat2(ChallengeResponse.apply)
}

case class ChallengeStatsResponse(meta: Meta, stats: Stats)

object ChallengeStatsResponse extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val statsFormat = jsonFormat2(core.Stats)
  implicit val format = jsonFormat2(ChallengeStatsResponse.apply)
}

case class ChallengeStats(meta: Meta, stats: Stats)

trait GameRoute {

  lazy val gameRoute =
    pathPrefix("game" / Segment) { nick =>
      startGame(nick) ~
        getChallenge(nick) ~
        getCurrentChallenge(nick) ~
        path("challenge" / Segment) { challenge =>
          getChallengeWithUuid(nick)(challenge) ~
            postChallenge(nick)(challenge)
        }
    }

  private val makeMeta: String => JessLink => Meta = nick => link =>
    Meta(
      current = s"/game/$nick/challenge/$link",
      stats = s"/game/$nick/challenge"
    )

  private lazy val startGame: String => Route =
    nick =>
      (path("start") & get) {
        complete {
          val respF = (gameActorRef ? GameActor.Join(nick)).mapTo[Xor[SomeError, Challenge]]

          respF.map {
            case resp => resp.fold(
              err =>
                HttpResponse(StatusCodes.BadRequest, entity = err.toString),
              challenge => HttpResponse(StatusCodes.OK, entity = ChallengeResponse(
                meta = makeMeta(nick)("link_change_me"),
                challenge
              ).toJson.prettyPrint)
            )
          }
        }
      }

  private lazy val getChallenge: String => Route =
    nick =>
      (path("challenge") & get) {
        complete {
          for {
            jessLink <- (gameActorRef ? GameActor.Current(nick)).mapTo[JessLink]
            stats <- (gameActorRef ? GameActor.Stats(nick)).mapTo[Stats]
          } yield {
            ChallengeStatsResponse(meta = makeMeta(nick)(jessLink), stats)
          }
        }
      }

  private lazy val getCurrentChallenge: String => Route =
    nick =>
      (path("challenge" / "current") & get) {
        complete {
          for {
            jessLink <- (gameActorRef ? GameActor.Current(nick)).mapTo[JessLink]
            challenge <- (gameActorRef ? GameActor.GetChallenge(nick, jessLink)).mapTo[Challenge]
          } yield {
            ChallengeResponse(meta = makeMeta(nick)(jessLink), challenge)
          }
        }
      }

  private lazy val getChallengeWithUuid: String => String => Route =
    nick => challenge =>
      get {
        complete {
          import ChallengeFormat._
          (gameActorRef ? GameActor.GetChallenge(nick, challenge)).mapTo[Challenge]
        }
      }

  private lazy val postChallenge: String => String => Route =
    nick => challenge =>
      post {
        entity(as[PostAnswerRequest]) { par =>
          complete {
            val resp = (gameActorRef ? GameActor.PostChallenge(nick, challenge, par.answer)).mapTo[Xor[SomeError, Challenge]]
            resp.map {
              case Xor.Right(_) => StatusCodes.OK -> "Correct Answer"
              case Xor.Left(err) => StatusCodes.BadRequest -> err.toString
            }
          }
        }
      }

  implicit val timeout: Timeout

  def gameActorRef: ActorRef

}
