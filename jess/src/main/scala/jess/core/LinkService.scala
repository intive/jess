package com.blstream.jess
package core

import java.util.UUID

trait LinkService {
  def genLink: JessLink = UUID.randomUUID.toString
}