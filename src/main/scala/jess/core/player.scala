package com.blstream.jess
package core.state

import cats.data.{ State, Xor }
import cats.syntax.xor._

import core._
import monocle.macros.GenLens

final case class PlayerState(
    nick: Option[String],
    points: Int = 0,
    attempts: Int = 0,
    current: String,
    challenges: Map[String, Challenge]
) {
  val challenge = challenges(current)
}

final case class Challenge(title: String, description: String, assignment: String, level: Int, answer: String, link: Option[String])

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

final case class StateTransitionError(message: String) extends SomeError
case object IncorrectAnswer extends SomeError

trait NickValidator {

  val validate: String => Xor[SomeError, String] =
    nick =>
      for {
        _ <- notEmpty(nick)
        _ <- unique(nick)
      } yield nick

  private val notEmpty: String => Xor[SomeError, String] =
    nick =>
      if (nick.isEmpty) EmptyNickError.left
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
            val ch = nextChallenge(ps.challenge.level)
            (ps.copy(nick = Some(nick), current = ch.link.get, challenges = ps.challenges ++ Map(ch.link.get -> ch)), ch)
          }
        )
      }

  val answerChallenge: Answer => State[PlayerState, Xor[SomeError, Challenge]] =
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

  val checkAnswer: Answer => State[PlayerState, Xor[SomeError, Unit]] =
    answer =>
      for {
        ps <- State.get[PlayerState]
      } yield {
        if (ps.challenges(answer.link).answer == answer.answer) ().right
        else IncorrectAnswer.left
      }

  val updatePoints: State[PlayerState, Xor[SomeError, Int]] = State { ps =>
    {
      val _ps = incPoints(ps)
      (_ps, _ps.points.right)
    }
  }

  val newChallenge: State[PlayerState, Xor[SomeError, Challenge]] = State { ps =>
    {
      val challenge = nextChallenge(ps.challenge.level + 1)
      val add: PlayerState => PlayerState = addChallenge(_)(challenge)
      val set: PlayerState => PlayerState = setCurrent(_)(challenge)
      val _ps = (add andThen set)(ps)
      (_ps, _ps.challenge.right)
    }
  }

  val incrementAttempts: State[PlayerState, Int] = State { ps =>
    {
      val _ps = incAttempt(ps)
      (_ps, _ps.attempts)
    }
  }

  val initialState: Challenge => PlayerState =
    ch => PlayerState(nick = None, current = ch.link.get, challenges = Map(ch.link.get -> ch))

  private val _attempts = GenLens[PlayerState](_.attempts)
  private val _points = GenLens[PlayerState](_.points)
  private val _challenges = GenLens[PlayerState](_.challenges)
  private val _current = GenLens[PlayerState](_.current)

  private val incAttempt: PlayerState => PlayerState = ps => _attempts.modify(_ + 1)(ps)
  private val incPoints: PlayerState => PlayerState = ps => _points.modify(_ + 10)(ps)
  private val setCurrent: PlayerState => Challenge => PlayerState = ps => ch => _current.set(ch.link.get)(ps)
  private val addChallenge: PlayerState => Challenge => PlayerState = ps => ch => _challenges.modify(x => x ++ Map(ch.link.get -> ch))(ps)

}

