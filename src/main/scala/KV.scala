package com.madewithtea.bottledynamo

import com.twitter.util.{Await, Future}

import io.circe._
import io.circe.syntax._

import scala.collection.mutable

case class TableDef(name: String, kv: KV)

// --- KV Abstraction ---

trait KV {
  def create(table: String): Future[Unit]

  def createRange(table: String): Future[Unit]

  def delete(table: String): Future[Unit]

  def remove(table: String, pk: String): Future[Unit]

  def remove(table: String, pk: String, rk: Long): Future[Unit]

  def put(table: String, pk: String, buf: String): Future[Unit]

  def put(table: String, pk: String, rk: Long, buf: String): Future[Unit]

  def get(table: String, pk: String): Future[Option[String]]

  def get(table: String, pk: String, rk: Long): Future[Option[String]]

  def query(table: String, pk: String, start: Option[Long], end: Option[Long]): Future[Seq[(Long, String)]]

  def print(table: String): Future[String]
}

class Store(tables: Map[String, TableDef]) {
  def table[A](table: String)
              (implicit encoder: io.circe.Encoder[A], decoder: io.circe.Decoder[A]): Table[A] =
    new Table[A](tables(table))

  def row[A](table: String, pk: String)
            (implicit encoder: io.circe.Encoder[A], decoder: io.circe.Decoder[A]): Row[A] =
    new Row[A](new Table[A](tables(table)), pk
    )
}

object Store {
  def forKV(kv: KV, tables: Seq[String]) =
    new Store(tables.map { name => (name, TableDef(name, kv)) }.toMap)
}

class Row[A](table: Table[A], pk: String)
            (implicit encoder: io.circe.Encoder[A], decoder: io.circe.Decoder[A]) {
  def put(config: A): Future[Unit] = table.put(pk)(config)

  def put(rk: Long, config: A): Future[Unit] = table.put(pk, rk)(config)

  def query(start: Option[Long], end: Option[Long]) = table.query(pk, start, end)

  def remove: Future[Unit] = table.remove(pk)

  def get: Future[Option[A]] = table.get(pk)

  override def toString: String = Await.result(table.get(pk)).toString
}

class Table[A](table: TableDef) {
  def logOp(info: String) = println(s"TABLE ${table.name} " + info)

  def create: Future[Unit] = {
    logOp("CREATE")
    table.kv.create(table.name)
  }

  def createRange: Future[Unit] = {
    logOp("CREATE RANGE")
    table.kv.createRange(table.name)
  }

  def delete: Future[Unit] = {
    logOp("DELETE")
    table.kv.delete(table.name)
  }

  def remove(pk: String): Future[Unit] = {
    logOp(s"REMOVE RECORD $pk")
    table.kv.remove(table.name, pk)
  }

  def put(pk: String, rk: Long)(a: A)(implicit encoder: io.circe.Encoder[A]): Future[Unit] = {
    logOp(s"PUT RANGE RECORD $pk $rk")
    table.kv.put(table.name, pk, rk, a.asJson.noSpaces)
  }

  def put(pk: String)(a: A)(implicit encoder: io.circe.Encoder[A]): Future[Unit] = {
    logOp(s"PUT RECORD $pk ")
    table.kv.put(table.name, pk, a.asJson.noSpaces)
  }

  def get(pk: String)(implicit decoder: io.circe.Decoder[A]): Future[Option[A]] = {
    logOp(s"GET RECORD $pk")
    decode(table.kv.get(table.name, pk))
  }

  def get(pk: String, rk: Long)(implicit decoder: io.circe.Decoder[A]): Future[Option[A]] = {
    logOp(s"GET RECORD $pk $rk")
    decode(table.kv.get(table.name, pk, rk))
  }

  def query(pk: String, start: Option[Long], end: Option[Long])
           (implicit decoder: io.circe.Decoder[A]): Future[Seq[(Long, A)]] = {
    logOp(s"GET RANGE $pk $start $end")
    for {
      result <- table.kv.query(table.name, pk, start, end)
    } yield result.map { case (r, v) => (r, Json.String.decode[A](v)) }
  }

  def decode(optRecord: Future[Option[String]])(implicit decoder: io.circe.Decoder[A]) = for {
    record <- optRecord
  } yield record.map(r => Json.String.decode[A](r))

  def row(pk: String)(implicit encoder: io.circe.Encoder[A], decoder: io.circe.Decoder[A])
  = new Row(this, pk)

  override def toString: String = Await.result(table.kv.print(table.name))
}

// --- In-memory KV for tests ---
class InMemoryKVImpl extends KV {
  type RangeValue = Map[Long, String]

  val storage = mutable.Map[String, mutable.Map[String, RangeValue]]()

  override def create(table: String): Future[Unit] = {
    println(s"CREATE ${table}")
    storage.put(table, mutable.Map[String, RangeValue]())
    Future.Unit
  }

  override def createRange(table: String) = create(table)

  // get value
  override def get(table: String, pk: String): Future[Option[String]] = {
    val value = storage(table).get(pk).flatMap(_.get(-1))
    Future.value(value)
  }

  override def get(table: String, pk: String, rk: Long) = {
    val value = storage(table).get(pk).flatMap(_.get(rk))
    Future.value(value)
  }

  override def put(table: String, pk: String, value: String): Future[Unit] = {
    println(s"value $value")
    storage(table).put(pk, Map[Long, String]((-1L, value)))
    Future.Unit
  }

  override def put(table: String, pk: String, rk: Long, value: String): Future[Unit] = {
    val current = storage(table).getOrElse(pk, Map[Long, String]())
    storage(table).put(pk, current.+((rk, value)))
    Future.Unit
  }

  override def delete(table: String): Future[Unit] = {
    Future.value(storage.remove(table))
  }

  override def remove(table: String, pk: String): Future[Unit] = {
    Future.value(storage(table).remove(pk))
  }

  override def remove(table: String, pk: String, rk: Long): Future[Unit] = {
    storage(table).put(pk, storage(table)(pk).-(rk))
    Future.Unit
  }

  override def print(table: String): Future[String] = {
    Future.value(storage(table).toString())
  }

  override def query(table: String, pk: String, start: Option[Long],
                     end: Option[Long]): Future[Seq[(Long, String)]] = {

    val rks = storage(table).getOrElse(pk, Seq())
    val result = (start, end) match {
      case (Some(s), Some(e)) => rks.filter { case (rk, _) => (s <= rk) & (rk <= e) }
      case (Some(s), None) => rks.filter { case (rk, _) => s <= rk }
      case (None, Some(e)) => rks.filter { case (rk, _) => rk <= e }
      case (None, None) => rks
    }
    Future.value(result.toSeq)
  }
}
