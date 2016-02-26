package com.blstream.jess
package core.state

import cats.SemigroupK
import cats.data.{ NonEmptyList, State, StateT, Xor }
import cats.syntax.xor._

import core._

final case class PlayerState(
  nick: Option[String],
  points: Int = 0,
  attempts: Int = 0,
  chans: ChallengeWithAnswer
)

final case class ChallengeWithAnswer(level: Int, challenge: Challenge, answer: String)

final case class Challenge(title: String, description: String, assignment: String)

object PlayerLogic {

  sealed trait PlayerAction

  case class StartGame(nick: String) extends PlayerAction

  case class Next(link: JessLink) extends PlayerAction

  case class Answer(link: JessLink, answer: String) extends PlayerAction

  case object Current extends PlayerAction

  case object Stats extends PlayerAction

}
sealed trait SomeError

final case object EmptyNickError extends SomeError

final case object AlreadyTakenNickError extends SomeError

final case class StateTransitionError(message: String) extends SomeError
final case object IncorrectAnswer extends SomeError

trait NickValidator {

  val validate: String => Xor[SomeError, String] =
    nick =>
      for {
        _ <- notEmpty(nick)
        _ <- unique(nick)
      } yield nick

  private val notEmpty: String => Xor[SomeError, String] =
    nick =>
      if (nick.isEmpty || nick == "foo") EmptyNickError.left
      else nick.right

  private val unique: String => Xor[SomeError, String] =
    nick =>
      nick.right
}

trait PlayerLogic {
  self: ChallengeService with NickValidator =>

  import PlayerLogic._

  val initGame: State[PlayerState, Unit] = State(ps => (ps, ()))

  val startGame: StartGame => Xor[SomeError, State[PlayerState, Challenge]] =
    start =>
      for {
        nick <- validate(start.nick)
      } yield {
        State(
          ps => {
            val chans = nextChallenge(ps.chans.level)
            (ps.copy(nick = Some(nick), chans = chans), chans.challenge)
          }
        )
      }

  val checkAnswer: Answer => State[PlayerState, Xor[SomeError, Unit]] = answer => for {
    foo <- State.get[PlayerState]
  } yield {
    if (foo.chans.answer == answer.answer) {
      ().right
    } else {
      IncorrectAnswer.left
    }
  }

  val answerChallenge: Answer => State[PlayerState, Xor[SomeError, Challenge]] =
    answer => for {
      _ <- incrementAttempts
      ans <- checkAnswer(answer)
      challenge <- ans.fold(
        err => State((s: PlayerState) => (s, err.left)),
        _ => for {
          _ <- updatePoints
          challenge <- newChallenge
        } yield challenge
      )
    } yield challenge

  val updatePoints: State[PlayerState, Xor[SomeError, Unit]] =
    State(ps => (ps.copy(points = ps.points + 10), ().right))

  private val newChallenge: State[PlayerState, Xor[SomeError, Challenge]] =
    State(ps => {
      val ch = nextChallenge(ps.chans.level + 1)
      (ps.copy(chans = ch), ch.challenge.right)
    })

  private val incrementAttempts: State[PlayerState, Int] =
    State(ps => (ps.copy(attempts = ps.attempts + 1), ps.attempts))
}
