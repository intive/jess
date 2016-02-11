package jess
package core

import java.util.UUID

trait StartGameService {

  def startGame: String => JessLink =
    nick => {
      UUID.randomUUID().toString
    }

}
