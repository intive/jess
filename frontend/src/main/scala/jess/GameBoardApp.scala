package com.blstream.jess


import org.scalajs.dom
import org.scalajs.dom.raw._

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
import scalatags.JsDom.all._


case class Score(player: String, points: Int)

case class GameEvent(event: String)

@JSExport
object GameBoard extends JSApp {

  val joinButton = dom.document.getElementById("ws-join").asInstanceOf[HTMLButtonElement]
  val wsAddressInput = dom.document.getElementById("ws-address").asInstanceOf[HTMLInputElement]
  val rnd = util.Random
  val ev = scala.collection.mutable.ArrayBuffer.empty[GameEvent]
  val sc = scala.collection.mutable.HashMap.empty[String, Score]

  @JSExport
  def main(): Unit = {

    wsAddressInput.placeholder = "127.0.0.1:8090/stream"

    joinButton.onclick = { (event: MouseEvent) =>
      consoleLog(wsAddressInput.value)
      joinBoardStream(wsAddressInput.value)
      event.preventDefault()
    }
  }

  private def joinBoardStream(ws: String): Unit = {
    val ws: WebSocket = new dom.WebSocket(s"ws://${wsAddressInput.value}")
    ws.binaryType = "arraybuffer"
    ws.onmessage = (event: MessageEvent) => {

      val message = TypedArrayBuffer.wrap(event.data.asInstanceOf[ArrayBuffer])
      protocol.decode(message) match {
        case protocol.PlayerJoinsGame(player) =>
          ev += GameEvent(s"Player $player has joined the game")
          sc.put(player, Score(player, 0))
          consoleLog(player)
        case protocol.PlayerScoresPoint(player, point) =>
          ev += GameEvent(s"Player $player has scored point")
          sc.put(player, Score(player, point))
          consoleLog(s"$player $point")
        case protocol.Ping(_) =>
          consoleLog("ping")
      }


      val eventsPanel = dom.document.getElementById("game-board-events-panel")
      eventsPanel.replaceChild(eventList(ev).render, eventsPanel.childNodes(2))

      val scorePanel = dom.document.getElementById("game-board-score-panel")
      scorePanel.replaceChild(scoreBoard(sc.values.toSeq), scorePanel.childNodes(2))

    }
  }


  private def scoreBoard(scores: Seq[Score]) = {
    table(
      `class` := "table table-striped",
      thead(
        tr(
          th("Player"),
          th("Score")
        )
      ),
      tbody(
        scores.map(s =>
          tr(
            td(s.player),
            td(s.points)
          )
        )
      )
    ).render
  }


  private def eventList(events: Seq[GameEvent]) = {
    ul(
      `class` := "list-group",
      events.reverse.map(e => li(
        `class` := "list-group-item",
        e.event
      ))
    )
  }

}

