/**
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
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


