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

  val initGame: State[Option[PlayerState], Unit] = State(ps => (ps, ()))

  val startGame: StartGame => State[Option[PlayerState], Xor[SomeError, ChallengeWithAnswer]] =
    start =>
      State(ps => {
        for {
          nick <- validate(start.nick)
          challenge <- Xor.fromOption(nextChallenge(0), NoChallengesError)
        } yield (Some(PlayerState(nick = nick, current = challenge.link.get, challenges = Map(challenge.link.get -> challenge))), challenge.right)
      }.fold(
        err => (ps, err.left),
        y => y
      ))

  val answerChallenge: Answer => State[Option[PlayerState], Xor[SomeError, ChallengeWithAnswer]] =
    answer => for {
      _ <- incrementAttempts
      ans <- checkAnswer(answer)
      challenge <- ans.fold(
        err => State((s: Option[PlayerState]) => (s, err.left)),
        _ => for {
          _ <- updatePoints
          ch <- newChallenge
        } yield ch
      )
    } yield challenge

  val checkAnswer: Answer => State[Option[PlayerState], Xor[SomeError, Unit]] =
    answer =>
      for {
        psMaybe <- State.get[Option[PlayerState]]
      } yield {
        psMaybe.map { ps =>
          if (ps.challenges(answer.link).answer == answer.answer) ().right
          else IncorrectAnswer.left
        }.getOrElse(StateNotInitialized.left)
      }

  val updatePoints: State[Option[PlayerState], Xor[SomeError, Int]] =
    State { psMaybe =>
      psMaybe.map { ps =>
        {
          val _ps = incPoints(ps)
          (Some(_ps), _ps.points.right)
        }
      }.getOrElse((psMaybe, StateNotInitialized.left))
    }

  val newChallenge: State[Option[PlayerState], Xor[SomeError, ChallengeWithAnswer]] =
    State { psMaybe =>
      {
        val resPsXor = for {
          ps <- Xor.fromOption(psMaybe, StateNotInitialized)
          challenge <- Xor.fromOption(nextChallenge(ps.challenge.level + 1), GameFinished)
        } yield {
          val add: PlayerState => PlayerState = addChallenge(_)(challenge)
          val set: PlayerState => PlayerState = setCurrent(_)(challenge)
          val _ps = (add andThen set)(ps)
          _ps
        }
        (if (resPsXor.isLeft) psMaybe else resPsXor.toOption, resPsXor.map(ns => ns.challenge))
      }
    }

  val incrementAttempts: State[Option[PlayerState], Xor[SomeError, Int]] =
    State { psMaybe =>
      psMaybe.map { ps =>
        {
          val _ps = incAttempt(ps)
          (Some(_ps), _ps.attempts.right)
        }
      }.getOrElse((psMaybe, StateNotInitialized.left))
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

