package com.blstream.jess

import java.nio.ByteBuffer

import scodec.bits.BitVector
import scodec.codecs._

package object protocol {

  import scodec.Codec

  sealed trait WsApi

  object WsApi {
    implicit val discriminated: Discriminated[WsApi, Int] = Discriminated(uint8)
  }

  case class PlayerJoinsGame(player: String) extends WsApi

  object PlayerJoinsGame {
    implicit val codec: Codec[PlayerJoinsGame] = {
      "player" | utf8
    }.as[PlayerJoinsGame]

    implicit val discriminator: Discriminator[WsApi, PlayerJoinsGame, Int] = Discriminator(1)
  }

  case class PlayerScoresPoint(player: String, point: Int) extends WsApi

  object PlayerScoresPoint {
    implicit val codec: Codec[PlayerScoresPoint] = {
      ("player" | utf8) :: ("score" | uint16)
    }.as[PlayerScoresPoint]

    implicit val discriminator: Discriminator[WsApi, PlayerScoresPoint, Int] = Discriminator(2)
  }

  case class Ping(ts: Long) extends WsApi

  object Ping {
    implicit val codec: Codec[Ping] = {
      "ts" | long(64)
    }.as[Ping]
    implicit val discriminator: Discriminator[WsApi, Ping, Int] = Discriminator(3)
  }

  def encode(a: WsApi): ByteBuffer = {
    val codec = Codec.coproduct[WsApi].discriminatedByIndex(uint8)
    codec.encode(a).require.toByteBuffer
  }

  def decode(bb: ByteBuffer): WsApi = {
    val choiceCodec = Codec.coproduct[WsApi].discriminatedByIndex(uint8)
    choiceCodec.decode(BitVector(bb)).require.value
  }
}
