package com.blstream.jess
package core.state

import cats.data.{ State, Xor }
import cats.syntax.xor._

import core._
import monocle.macros.GenLens

final case class PlayerState(
  nick: String,
  points: Int = 0,
  attempts: Int = 0,
  current: String,
  challenges: Map[String, ChallengeWithAnswer]
) extends {
  val challenge = challenges(current)
}

abstract class ChallengeBase {
  val title: String
  val description: String
  val assignment: String
  val level: Int
  val link: Option[String]
}

case class Challenge(
  override val title: String,
  override val description: String,
  override val assignment: String,
  override val level: Int,
  override val link: Option[String]
) extends ChallengeBase

final case class ChallengeWithAnswer(
    override val title: String,
    override val description: String,
    override val assignment: String,
    override val level: Int,
    override val link: Option[String],
    answer: String
) extends ChallengeBase {

  def withoutAnswer = Challenge(title, description, assignment, level, link)

}

object PlayerLogic {

  sealed trait PlayerAction
  case class StartGame(nick: String) extends PlayerAction
  case class GetChallenge(link: JessLink) extends PlayerAction
  case class Answer(link: JessLink, answer: String) extends PlayerAction
  case object Current extends PlayerAction
  case object Stats extends PlayerAction

}

sealed trait SomeError
case object EmptyNickError extends SomeError
case object AlreadyTakenNickError extends SomeError
case object GameFinished extends SomeError
case object NoChallengesError extends SomeError
case object StateNotInitialized extends SomeError
case object GameAlreadyStarted extends SomeError

final case class StateTransitionError(message: String) extends SomeError
case object IncorrectAnswer extends SomeError
case object AlreadyAnswered extends SomeError

trait StartGameValidator {

  val validate: String => Option[PlayerState] => SomeError Xor String =
    nick => ps =>
      for {
        _ <- notEmpty(nick)
        _ <- unique(nick)
        _ <- notYetStarted(ps)
      } yield nick

  private val notEmpty: String => SomeError Xor String =
    nick =>
      if (nick.isEmpty) EmptyNickError.left
      else nick.right

  private val unique: String => SomeError Xor String =
    nick =>
      nick.right

  private val notYetStarted: Option[PlayerState] => SomeError Xor Option[PlayerState] =
    stateMaybe =>
      if (stateMaybe.isEmpty) stateMaybe.right
      else GameAlreadyStarted.left
}

trait PlayerLogic {
  self: ChallengeService with StartGameValidator =>

  import PlayerLogic._

  val startGame: StartGame => State[Option[PlayerState], SomeError Xor ChallengeServiceResponse] =
    start =>
      State(ps => {
        for {
          nick <- validate(start.nick)(ps)
          challengeResponse <- nextChallenge(0)
        } yield challengeResponse match {
          case lcs @ LastChallengeSolved => (ps, lcs.right) //will never happen
          case nc @ NextChallenge(challenge) =>
            (
              Some(PlayerState(nick = nick, current = challenge.link.get, challenges = Map(challenge.link.get -> challenge))),
              nc.right
            )
        }
      }.fold(
        err => (ps, err.left),
        y => y
      ))

  val answerChallenge: Answer => State[PlayerState, SomeError Xor ChallengeServiceResponse] =
    answer => for {
      _ <- incrementAttempts
      ans <- checkAnswer(answer)
      challenge <- ans.fold(
        err => State((s: PlayerState) => (s, err.left)),
        _ => for {
          _ <- updatePoints
          ch <- newChallenge
        } yield ch
      )
    } yield challenge

  val checkAnswer: Answer => State[PlayerState, SomeError Xor Unit] =
    answer =>
      State(ps => {
        val check =
          if (answer.link != ps.challenge.link.get) AlreadyAnswered.left
          else if (ps.challenges(answer.link).answer == answer.answer) ().right
          else IncorrectAnswer.left
        (ps, check)
      })

  val updatePoints: State[PlayerState, SomeError Xor Int] =
    State(ps =>
      {
        val _ps = incPoints(ps)
        (_ps, _ps.points.right)
      })

  val newChallenge: State[PlayerState, SomeError Xor ChallengeServiceResponse] =
    State(ps =>
      {
        for {
          challengeResponse <- nextChallenge(ps.challenge.level + 1)
        } yield {
          challengeResponse match {
            case lcs @ LastChallengeSolved => (ps, lcs.right)
            case nc @ NextChallenge(challenge) => {
              val add: PlayerState => PlayerState = addChallenge(_)(challenge)
              val set: PlayerState => PlayerState = setCurrent(_)(challenge)
              val _ps = (add andThen set)(ps)
              (_ps, nc.right)
            }
          }
        }
      }.fold(
        err => (ps, err.left),
        y => y
      ))

  val incrementAttempts: State[PlayerState, SomeError Xor Int] =
    State { ps =>
      {
        val _ps = incAttempt(ps)
        (_ps, _ps.attempts.right)
      }
    }

  private val _attempts = GenLens[PlayerState](_.attempts)
  private val _points = GenLens[PlayerState](_.points)
  private val _challenges = GenLens[PlayerState](_.challenges)
  private val _current = GenLens[PlayerState](_.current)

  private val incAttempt: PlayerState => PlayerState = ps => _attempts.modify(_ + 1)(ps)
  private val incPoints: PlayerState => PlayerState = ps => _points.modify(_ + 10)(ps)
  private val setCurrent: PlayerState => ChallengeWithAnswer => PlayerState = ps => ch => _current.set(ch.link.get)(ps)
  private val addChallenge: PlayerState => ChallengeWithAnswer => PlayerState = ps => ch => _challenges.modify(x => x ++ Map(ch.link.get -> ch))(ps)

}

