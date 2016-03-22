package jess.core

import akka.persistence.PersistentActor
import com.blstream.jess.core.state.ChallengeWithAnswer

sealed trait AdminCommands
case class AddChallenge(challenge: ChallengeWithAnswer) extends AdminCommands

class AdminActor extends PersistentActor {
  override def persistenceId: String = "admin-actor"

  override def receiveCommand: Receive = {
    case AddChallenge(chans) =>
  }

  override def receiveRecover: Receive = ???
}
