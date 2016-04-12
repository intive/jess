package com.blstream.jess
package api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import cats.data.Xor
import core._
import spray.json._

import scala.concurrent.ExecutionContext
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
  implicit val formatChallenge: RootJsonFormat[ChallengeWithAnswer] = jsonFormat7(ChallengeWithAnswer)
}

object ChallengeFormat extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val formatChallenge: RootJsonFormat[Challenge] = jsonFormat6(Challenge)
}

case class ChallengeResponse(meta: Meta, challenge: Option[Challenge], answer: Option[Answer], gameStatus: Option[GameMode])

object ChallengeResponse extends SprayJsonSupport with DefaultJsonProtocol {
  import ChallengeFormat._
  implicit val gameModeFormat = new RootJsonFormat[GameMode] {
    def write(obj: GameMode): JsValue = obj match {
      case Playing => Playing.toString.toJson
      case Finished => Finished.toString.toJson
    }
    def read(json: JsValue): GameMode = ???
  }
  implicit val answerFormat = jsonFormat3(Answer)
  implicit val format = jsonFormat4(ChallengeResponse.apply)
}

case class ChallengeStatsResponse(meta: Meta, stats: PlayerStatus)

object ChallengeStatsResponse extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val statsFormat = jsonFormat3(PlayerStatus)
  implicit val format = jsonFormat2(ChallengeStatsResponse.apply)
}

case class Answer(correct: Boolean, points: String, error: Option[String])

sealed trait GameMode
case object Playing extends GameMode
case object Finished extends GameMode

trait GameRoute {

  gameService: GameService =>

  def gameRoute(implicit ec: ExecutionContext, timeout: Timeout, gameStateRef: GameStateRef, scoreRouterRef: ScoreRouterRef) =
    pathPrefix("game" / Segment) { nick =>
      startGame(nick) ~
        getChallengeStats(nick) ~
        path("challenge" / Segment) { challenge =>
          getChallenge(nick, challenge) ~
            answerChallenge(nick, challenge)
        }
    }

  private val makeMeta: String => JessLink => Meta = nick => link =>
    Meta(
      current = s"/game/$nick/challenge/$link",
      stats = s"/game/$nick/challenge",
      link = link
    )

  private def startGame(nick: String)(implicit ec: ExecutionContext, timeout: Timeout, gameStateRef: GameStateRef, scoreRouterRef: ScoreRouterRef): Route =
    (path("start") & put) {
      complete {
        val resp = joinGameIo(nick)
        resp.map(responseMapper(nick, _))
      }
    }

  private def getChallengeStats(nick: String)(implicit ec: ExecutionContext, timeout: Timeout, gameStateRef: GameStateRef, scoreRouterRef: ScoreRouterRef): Route =
    (path("challenge") & get) {
      complete {
        import ChallengeFormat._
        for {
          jessLinkXor <- getCurrentIo(nick)
          statsXor <- getStatsIo(nick)
        } yield {
          (for {
            jessLink <- jessLinkXor
            stats <- statsXor
          } yield ChallengeStatsResponse(meta = makeMeta(nick)(jessLink.getOrElse("")), stats)) match {
            case Xor.Left(err) => err.toString.toJson.prettyPrint
            case Xor.Right(lnk) => lnk.toJson.prettyPrint
          }
        }
      }
    }

  private def getChallenge(nick: Nick, challenge: JessLink)(implicit ec: ExecutionContext, timeout: Timeout, gameStateRef: GameStateRef, scoreRouterRef: ScoreRouterRef): Route =
    get {
      complete {
        import ChallengeFormat._
        getChallengeIo(nick, challenge).map {
          case Xor.Left(err) => err.toString.toJson.prettyPrint
          case Xor.Right(ch) => ch.toJson.prettyPrint
        }
      }
    }

  private def answerChallenge(nick: Nick, challenge: JessLink)(implicit ec: ExecutionContext, timeout: Timeout, gameStateRef: GameStateRef, scoreRouterRef: ScoreRouterRef): Route =
    put {
      entity(as[PostAnswerRequest]) { par =>
        complete {
          val resp = answerGameChallengeIo(nick, challenge, par.answer)
          resp.map(responseMapper(nick, _))
        }
      }
    }

  def responseMapper(nick: String, xor: Xor[SomeError, ChallengeServiceResponse]) = {
    xor match {
      case Xor.Right(NextChallenge(Some(solved), Some(challenge))) =>
        HttpResponse(StatusCodes.OK, entity = ChallengeResponse(
          meta = makeMeta(nick)(challenge.link.getOrElse("")),
          challenge = Some(challenge.withoutAnswer),
          answer = Some(Answer(correct = true, points = s"+${solved.challengePoints}", None)),
          gameStatus = Some(Playing)
        ).toJson.prettyPrint)
      case Xor.Right(NextChallenge(None, Some(challenge))) =>
        HttpResponse(StatusCodes.OK, entity = ChallengeResponse(
          meta = makeMeta(nick)(challenge.link.getOrElse("")),
          challenge = Some(challenge.withoutAnswer),
          answer = Some(Answer(correct = true, points = "0", None)),
          gameStatus = Some(Playing)
        ).toJson.prettyPrint)
      case Xor.Right(NextChallenge(Some(solved), None)) =>
        HttpResponse(StatusCodes.OK, entity = ChallengeResponse(
          meta = makeMeta(nick)(""),
          None,
          answer = Some(Answer(correct = true, points = s"+${solved.challengePoints}", None)),
          gameStatus = Some(Finished)
        ).toJson.prettyPrint)
      case Xor.Right(NextChallenge(None, None)) =>
        HttpResponse(StatusCodes.OK, entity = ChallengeResponse(
          meta = makeMeta(nick)(""),
          challenge = None,
          answer = Some(Answer(correct = false, points = "0", None)),
          gameStatus = Some(Playing)
        ).toJson.prettyPrint)
      case Xor.Left(err) =>
        HttpResponse(StatusCodes.OK, entity = ChallengeResponse(
          meta = makeMeta(nick)(""),
          challenge = None,
          answer = Some(Answer(correct = false, points = "0", Some(err.toString))),
          gameStatus = Some(Playing)
        ).toJson.prettyPrint)
    }
  }

}
