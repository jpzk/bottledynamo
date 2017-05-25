package com.madewithtea.bottledynamo

import io.circe.generic.auto._
import io.circe._
import io.circe.syntax._
import com.twitter.io.{Buf => TBuf}

object Json {

  def onFailure(error: io.circe.Error) = {
    println(error.getMessage)
    error.printStackTrace()
    throw error
  }

  object String {
    def encode[T](value: T)(implicit encoder: io.circe.Encoder[T]): String =
      value.asJson.noSpaces

    def decode[T](value: String)(implicit dec: io.circe.Decoder[T]): T =
      parser.parse(value).fold(onFailure, { parsed =>
        parsed.as[T].fold[T](onFailure, instance => instance)
      })
  }

  object Buf {

    def encode[T](value: T)(implicit encoder: io.circe.Encoder[T]): TBuf = {
      val json = value.asJson.noSpaces
      TBuf.Utf8(json)
    }

    def decode[T](value: TBuf)(implicit dec: io.circe.Decoder[T]): T = {
      val TBuf.Utf8(json) = value
      parser.parse(json).right.get.as[T].right.get
    }
  }

}


