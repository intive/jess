package com.blstream.jess
package core

import java.util.UUID

trait GameService {

  def generateJessLink: JessLink = UUID.randomUUID().toString

}
