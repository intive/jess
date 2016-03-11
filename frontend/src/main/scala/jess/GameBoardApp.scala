package com.blstream.jess


import org.scalajs.dom
import org.scalajs.dom.raw._

import scalajs.js.JSApp
import scalajs.js.annotation.JSExport
import scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
import scalatags.JsDom.all._


case class Score(player: String, points: Int)

case class GameEvent(event: String)

@JSExport
object GameBoard extends JSApp {

  val joinButton = dom.document.getElementById("ws-join").asInstanceOf[HTMLButtonElement]
  val wsAddressInput = dom.document.getElementById("ws-address").asInstanceOf[HTMLInputElement]
  val rnd = util.Random
  val ev = scala.collection.mutable.ArrayBuffer.empty[GameEvent]

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

      val bb = TypedArrayBuffer.wrap(event.data.asInstanceOf[ArrayBuffer])

      protocol.decode(bb) match {
        case protocol.PlayerJoinsGame(player) => consoleLog(player)
        case protocol.PlayerScoresPoint(player, point) => consoleLog(s"$player $point")
        case protocol.Ping(_) => consoleLog("ping")
      }



//      ev += (protocol.decode(bb.array) match {
//        case protocol.Ping(ts) => GameEvent(s"ping-$ts")
//      })
//
//      val ch = dom.document.getElementById("game-board-events-panel")
//      ch.replaceChild(eventList(ev).render, ch.childNodes(2))
//
//      val ch1 = dom.document.getElementById("game-board-score-panel")
//      ch1.replaceChild(scoreBoard(
//        List(
//          Score("Player-1", rnd.nextInt(100)), Score("Player-2", rnd.nextInt(100)), Score("Player-3", rnd.nextInt(100))
//        )), ch1.childNodes(2))
    }
  }


  private def scoreBoard(scores: List[Score]) = {
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

