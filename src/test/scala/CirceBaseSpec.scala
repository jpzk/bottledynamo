package com.madewithtea.bottledynamo

import com.twitter.util.{Await, Future}
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalatest.{FlatSpec, Matchers}

class CirceBaseSpec extends FlatSpec with Matchers {

  case class A(long: Long)

  it should "encode put to KV and get and decode" in {
    val inMemoryKVImpl = new InMemoryKVImpl()
    val tableDef = TableDef("testTable",inMemoryKVImpl)
    val table = new Table[A](tableDef)
    Await.ready(table.create)
    Await.ready(table.put("somekey")(A(1L)))
    Await.result(table.get("somekey")) shouldEqual Some(A(1L))
  }
}
