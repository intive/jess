package com.blstream.jess
package api

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.pattern._
import akka.util.Timeout
import cats.data.ValidatedNel
import core.state.{ Challenge, SomeError }
import cats.std.list._
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
      (path("start") & get) {
        complete {
          makeChallengeResponse(nick)
        }
      } ~ (path("challenge") & get) {
        complete {
          makeChallengeStatsResponse(nick)
        }
      } ~ (path("challenge" / "current") & get) {
        complete {
          makeCurrentChallengeResponse(nick)
        }
      } ~ path("challenge" / Segment) { challenge =>
        get {
          complete {
            import ChallengeFormat._
            (gameActorRef ? GameActor.GetChallenge(nick, challenge)).mapTo[Challenge]
          }
        } ~ post {
          entity(as[PostAnswerRequest]) { par =>
            complete {
              val resp = (gameActorRef ? GameActor.PostChallenge(nick, challenge, par.answer)).mapTo[ResponseAnswer]
              resp.map {
                case CorrectAnswer => StatusCodes.OK -> "Correct Answer"
                case IncorrectAnswer => StatusCodes.BadRequest -> "Wrong Answer"
              }
            }
          }
        }
      }
    }

  private val makeMeta: String => JessLink => Meta = nick => link =>
    Meta(
      current = s"/game/$nick/challenge/$link",
      stats = s"/game/$nick/challenge"
    )

  private val makeChallengeResponse: String => Future[HttpResponse] = nick => {
    val respF = (gameActorRef ? GameActor.Join(nick)).mapTo[ValidatedNel[SomeError, Challenge]]

    respF.map {
      case resp => resp.fold(
        err =>
          HttpResponse(StatusCodes.BadRequest, entity = err.unwrap.mkString("|dupa|")),
        challenge => HttpResponse(StatusCodes.OK, entity = ChallengeResponse(
          meta = makeMeta(nick)("link_change_me"),
          challenge
        ).toJson.prettyPrint)
      )
    }

  }

  private val makeChallengeStatsResponse: String => Future[ChallengeStatsResponse] = nick => for {
    jessLink <- (gameActorRef ? GameActor.Current(nick)).mapTo[JessLink]
    stats <- (gameActorRef ? GameActor.Stats(nick)).mapTo[Stats]
  } yield {
    ChallengeStatsResponse(meta = makeMeta(nick)(jessLink), stats)
  }

  private val makeCurrentChallengeResponse: String => Future[ChallengeResponse] = nick => for {
    jessLink <- (gameActorRef ? GameActor.Current(nick)).mapTo[JessLink]
    challenge <- (gameActorRef ? GameActor.GetChallenge(nick, jessLink)).mapTo[Challenge]
  } yield {
    ChallengeResponse(meta = makeMeta(nick)(jessLink), challenge)
  }

  implicit val timeout: Timeout

  def gameActorRef: ActorRef

}