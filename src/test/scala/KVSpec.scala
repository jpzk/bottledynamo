package com.madewithtea.bottledynamo

import com.twitter.util.{Await, Future}
import io.circe._
import io.circe.generic.auto._
import org.scalatest.{FlatSpec, Matchers}

case class Entity(a: Int)

class KVSpec extends FlatSpec with Matchers {
  behavior of "InMemoryKV"

  it should "create a table" in withTable { t =>
    Await.result(t.create)
  }

  it should "create a record and retrieve a record with PK" in withTable { t =>
    val r = for {
      _ <- t.create
      _ <- t.put("pk")(Entity(0))
      r <- t.get("pk")
    } yield r
    Await.result(r) shouldEqual Some(Entity(0))
  }

  it should "create a record and retrieve a record with PK and RK" in withTable { t =>
    val r = for {
      _ <- t.create
      _ <- t.put("pk", 0L)(Entity(0))
      r <- t.get("pk", 0L)
    } yield r
    Await.result(r) shouldEqual Some(Entity(0))
  }

  it should "create a record with a range 2 <= x" in withTable { t =>
    val r = for {
      _ <- prepareRangeTable(t)
      r <- t.query("pk", Some(2), None)
    } yield r
    Await.result(r) shouldEqual Seq(
      (2, Entity(2)),
      (3, Entity(3)),
      (4, Entity(4))
    )
  }

  it should "create a record with a range 2 <= x <= 3" in withTable { t =>
    val r = for {
      _ <- prepareRangeTable(t)
      r <- t.query("pk", Some(2), Some(3))
    } yield r
    Await.result(r) shouldEqual Seq(
      (2, Entity(2)),
      (3, Entity(3)))
  }

  it should "create a record with a range x <= 3" in withTable { t =>
    val r = for {
      _ <- prepareRangeTable(t)
      r <- t.query("pk", None, Some(3))
    } yield r
    Await.result(r) shouldEqual Seq(
      (1, Entity(1)),
      (2, Entity(2)),
      (3, Entity(3)))
  }

  def prepareRangeTable(t: Table[Entity]) = for {
    _ <- t.create
    _ <- t.put("pk", 1)(Entity(1))
    _ <- t.put("pk", 2)(Entity(2))
    _ <- t.put("pk", 3)(Entity(3))
    _ <- t.put("pk", 4)(Entity(4))
  } yield ()

  def withTable(test: Table[Entity] => Any) = {
    val inMemoryKVImpl = new InMemoryKVImpl()
    val store = Store.forKV(new InMemoryKVImpl(), Seq("table"))

    test(store.table[Entity]("table"))
  }

}
