package jess
package core

import java.util.UUID

trait GameService {

  def startGame: String => JessLink =
    nick => {
      UUID.randomUUID().toString
    }

}
