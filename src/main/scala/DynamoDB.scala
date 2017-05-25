package com.madewithtea.bottledynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.document.spec.{GetItemSpec, QuerySpec}
import com.amazonaws.services.dynamodbv2.document.{Item, PrimaryKey, RangeKeyCondition, DynamoDB => UnderlyingDynamoDB}
import com.amazonaws.services.dynamodbv2.model._
import com.twitter.util.Future

import scala.collection.JavaConverters._

class DynamoDB(db: AmazonDynamoDB, consistentRead: Boolean = false) extends KV {
  private val PRIMARY_KEY = "pk"
  private val RANGE_KEY = "rk"
  private val ATTR = "json"
  private val ddb = new UnderlyingDynamoDB(db)

  def createGeneral(table: String)(f: CreateTableRequest => CreateTableRequest): Future[Unit] = {
    val request = new CreateTableRequest()
      .withTableName(table)
      .withProvisionedThroughput(new ProvisionedThroughput()
        .withReadCapacityUnits(1L)
        .withWriteCapacityUnits(1L))
    f(request)

    Future.value(db.createTable(request)).unit
  }

  override def create(table: String) = createGeneral(table) { spec =>
    spec.withAttributeDefinitions(
      new AttributeDefinition(PRIMARY_KEY, ScalarAttributeType.S)).withKeySchema(
      new KeySchemaElement(PRIMARY_KEY, KeyType.HASH))
  }

  override def createRange(table: String) = createGeneral(table) { spec =>
    spec.withAttributeDefinitions(
      new AttributeDefinition(PRIMARY_KEY, ScalarAttributeType.S),
      new AttributeDefinition(RANGE_KEY, ScalarAttributeType.N))
      .withKeySchema(
        new KeySchemaElement(PRIMARY_KEY, KeyType.HASH),
        new KeySchemaElement(RANGE_KEY, KeyType.RANGE)
      )
  }

  override def delete(table: String): Future[Unit] = {
    Future.value(db.deleteTable(table)).unit
  }

  override def put(table: String, pk: String, buf: String): Future[Unit] = {
    val t = ddb.getTable(table)
    for {
      item <- Future.value(
        new Item()
          .withPrimaryKey(PRIMARY_KEY, pk)
          .withJSON(ATTR, buf)
      )
    } yield {
      t.putItem(item)
    }
  }

  override def get(table: String, pk: String): Future[Option[String]] = {
    val t = ddb.getTable(table)
    for {
      item <- Future.value(t.getItem(new GetItemSpec()
        .withPrimaryKey(PRIMARY_KEY, pk)
        .withAttributesToGet(ATTR)
        .withConsistentRead(consistentRead)))

    } yield {
      if (item.hasAttribute(ATTR))
        Some(item.getJSON(ATTR))
      else None
    }
  }

  override def remove(table: String, pk: String): Future[Unit] = {
    val t = ddb.getTable(table)
    Future.value(t.deleteItem(new PrimaryKey(PRIMARY_KEY, pk)))
  }

  override def print(table: String): Future[String] = ???

  override def remove(table: String, pk: String, rk: Long): Future[Unit] = {
    val t = ddb.getTable(table)
    Future.value(t.deleteItem(new PrimaryKey(PRIMARY_KEY, pk, RANGE_KEY, rk)))
  }

  override def put(table: String, pk: String, rk: Long, buf: String): Future[Unit] = {
    val t = ddb.getTable(table)
    for {
      item <- Future.value(
        new Item()
          .withPrimaryKey(PRIMARY_KEY, pk, RANGE_KEY, rk)
          .withJSON(ATTR, buf)
      )
    } yield {
      t.putItem(item)
    }
  }

  override def get(table: String, pk: String, rk: Long): Future[Option[String]] = {
    val t = ddb.getTable(table)
    for {
      item <- Future.value(t.getItem(new GetItemSpec()
        .withPrimaryKey(PRIMARY_KEY, pk, RANGE_KEY, rk)
        .withAttributesToGet(ATTR)
        .withConsistentRead(consistentRead)))

    } yield {
      if (item.hasAttribute(ATTR))
        Some(item.getJSON(ATTR))
      else None
    }
  }

  override def query(table: String, pk: String, start: Option[Long],
                     end: Option[Long]): Future[Seq[(Long, String)]] = {
    val t = ddb.getTable(table)

    def spec = new QuerySpec().withHashKey(PRIMARY_KEY, pk)

    val rkc = new RangeKeyCondition(RANGE_KEY)
    val qs = (start, end) match {
      case (Some(s), Some(e)) => spec.withRangeKeyCondition(rkc.between(s, e))
      case (Some(s), None) => spec.withRangeKeyCondition(rkc.gt(s))
      case (None, Some(e)) => spec.withRangeKeyCondition(rkc.lt(e))
      case (None, None) => spec
    }
    for {
      result <- Future.value(t.query(qs))
      pages <- Future.value(result.pages().iterator().asScala.toSeq)
      items <- Future.value(pages.flatMap { page => page.iterator().asScala.toSeq })
    } yield items.map { item =>
      (item.getLong(RANGE_KEY), item.getJSON(ATTR))
    }
  }
}


