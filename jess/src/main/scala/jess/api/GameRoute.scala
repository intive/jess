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
import core.state.{ ChallengeWithAnswer, Challenge, SomeError }
import core._
import spray.json._

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

case class Meta(current: String, stats: String, link: String)

object Meta extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val format = jsonFormat3(Meta.apply)
}

object ChallengeWithAnswerFormat extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val formatChallenge: RootJsonFormat[ChallengeWithAnswer] = jsonFormat6(ChallengeWithAnswer)
}

object ChallengeFormat extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val formatChallenge: RootJsonFormat[Challenge] = jsonFormat5(Challenge)
}

case class ChallengeResponse(meta: Meta, challenge: Option[Challenge], answer: Option[Answer], gameStatus: Option[String])

object ChallengeResponse extends SprayJsonSupport with DefaultJsonProtocol {
  import ChallengeFormat._
  implicit val answerFormat = jsonFormat3(Answer)
  implicit val format = jsonFormat4(ChallengeResponse.apply)
}

case class ChallengeStatsResponse(meta: Meta, stats: Stats)

object ChallengeStatsResponse extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val statsFormat = jsonFormat3(core.Stats)
  implicit val format = jsonFormat2(ChallengeStatsResponse.apply)
}

case class ChallengeStats(meta: Meta, stats: Stats)

case class Answer(correct: Boolean, points: String, error: Option[String])

trait GameRoute {

  gameService: GameService =>

  lazy val gameRoute =
    pathPrefix("game" / Segment) { nick =>
      startGame(nick) ~
        getChallengeStats(nick) ~
        path("challenge" / Segment) { challenge =>
          getChallenge(nick)(challenge) ~
            answerChallenge(nick)(challenge)
        }
    }

  private val makeMeta: String => JessLink => Meta = nick => link =>
    Meta(
      current = s"/game/$nick/challenge/$link",
      stats = s"/game/$nick/challenge",
      link = link
    )

  private lazy val startGame: String => Route =
    nick =>
      (path("start") & put) {
        complete {
          val resp = joinGameIo(nick)
          resp.map(responseMapper(nick, _))
        }
      }

  private lazy val getChallengeStats: String => Route =
    nick =>
      (path("challenge") & get) {
        complete {
          for {
            jessLink <- (gameActorRef ? GameActor.Current(nick)).mapTo[Option[JessLink]]
            stats <- (gameActorRef ? GameActor.Stats(nick)).mapTo[Stats]
          } yield {
            ChallengeStatsResponse(meta = makeMeta(nick)(jessLink.getOrElse("")), stats)
          }
        }
      }

  private lazy val getChallenge: String => String => Route =
    nick => challenge =>
      get {
        complete {
          import ChallengeFormat._
          (gameActorRef ? GameActor.GetChallenge(nick, challenge)).mapTo[Challenge]
        }
      }

  private lazy val answerChallenge: String => String => Route =
    nick => challenge =>
      put {
        entity(as[PostAnswerRequest]) { par =>
          complete {
            val resp = (gameActorRef ? GameActor.PostChallenge(nick, challenge, par.answer)).mapTo[Xor[SomeError, ChallengeServiceResponse]]
            resp.map(responseMapper(nick, _))
          }
        }
      }

  def responseMapper(nick: String, xor: Xor[SomeError, ChallengeServiceResponse]) = {
    xor match {
      case Xor.Right(NextChallenge(challenge)) =>
        HttpResponse(StatusCodes.OK, entity = ChallengeResponse(
          meta = makeMeta(nick)(challenge.link.getOrElse("")),
          challenge = Some(challenge.withoutAnswer),
          //TODO read points from Challenge
          answer = Some(Answer(correct = true, points = "+10", None)),
          gameStatus = Some("playing")
        ).toJson.prettyPrint)
      case Xor.Right(LastChallengeSolved) =>
        HttpResponse(StatusCodes.OK, entity = ChallengeResponse(
          meta = makeMeta(nick)(""),
          None,
          //TODO read points from Challenge
          answer = Some(Answer(correct = true, points = "+10", None)),
          gameStatus = Some("finished")
        ).toJson.prettyPrint)
      case Xor.Left(err) =>
        HttpResponse(StatusCodes.OK, entity = ChallengeResponse(
          meta = makeMeta(nick)(""),
          challenge = None,
          //TODO read points from Challenge
          answer = Some(Answer(correct = false, points = "0", Some(err.toString))),
          gameStatus = Some("playing")
        ).toJson.prettyPrint)
    }
  }

  implicit val timeout: Timeout

  implicit val gameStateActor: GameStateRef

  val scoreRouter: ActorRef

  implicit lazy val scoreRouterRef: ScoreRouterRef = ScoreRouterRef(scoreRouter)

  def gameActorRef: ActorRef

}
