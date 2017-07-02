# bottledynamo

![Build Status](https://travis-ci.org/jpzk/bottledynamo.svg?branch=master) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/4134b7f384254ab9b8cceae6d986836c)](https://www.codacy.com/app/jpzk/bottledynamo?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=jpzk/bottledynamo&amp;utm_campaign=Badge_Grade) [![codecov](https://codecov.io/gh/jpzk/bottledynamo/branch/master/graph/badge.svg)](https://codecov.io/gh/jpzk/bottledynamo) [![License](http://img.shields.io/:license-Apache%202-grey.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt) [![GitHub stars](https://img.shields.io/github/stars/jpzk/bottledynamo.svg?style=flat)](https://github.com/jpzk/bottledynamo/stargazers) 

Bottle Dynamo is a good enough DynamoDB wrapper for putting and getting case classes in Scala. It uses Twitter's Futures and Circe as JSON serialization. Current features include:

* In-Memory backend and DynamoDB
* Support for exact-match get
* Support for range queries (numbers as range key)

## Dependency

Bottle Dynamo depends on Twitter Util Core (for futures), and on the AWS Java SDK DynamoDB (pullled in). Bottle Dynamo is available on Maven Central Repositories. 

    val bottledynamo = "com.madewithtea" %% "bottledynamo" % "1.0.0"

## In-Memory (for tests)

    import com.madewithtea.bottledynamo.{Store, Table, InMemoryKVImpl}
    import io.circe.generic.auto._

    case class SomeClass(field: String, number: Int)

    val store = storeForKV(new InMemoryKVImpl)
    val table = store.table[SomeClass]("sometable")
    
    val entry = for { 
      _ <- table.create
      _ <- table.put("PK")(SomeClass("value",2)))
    } yield table.get("PK")

    Await.result(entry)

## DynamoDB 

    import com.madewithtea.bottledynamo.{Store, Table, KV, DynamoDB, InMemoryKVImpl}
    import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
    
    val client = AmazonDynamoDBClientBuilder.standard()
          .withRegion(Regions.EU_CENTRAL_1)
          .build()

    val store = storeForKV(new DynamoDB(client))
    val table = store.table[SomeClass]("sometable")
    
    val entry = for { 
      _ <- table.create
      _ <- table.put("PK")(SomeClass("value",2)))
    } yield table.get("PK")

    Await.result(entry)

## DynamoDB and Range Tables

Create a table with DynamoDB interface first.

    import com.madewithtea.bottledynamo.{Store, Table, KV, DynamoDB, InMemoryKVImpl}
    import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
    
    val client = AmazonDynamoDBClientBuilder.standard()
          .withRegion(Regions.EU_CENTRAL_1)
          .build()

    val store = storeForKV(new DynamoDB(client))
    val table = store.table[SomeClass]("sometable")
    
    val entry = for { 
      _ <- table.put("PK", 10000)(SomeClass("value",2)))
    } yield table.get("PK",10000)

    Await.result(entry)

## DynamoDB and Range Queries

Create a table with DynamoDB interface first

    import com.madewithtea.bottledynamo.{Store, Table, KV, DynamoDB, InMemoryKVImpl}
    import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
    
    val client = AmazonDynamoDBClientBuilder.standard()
          .withRegion(Regions.EU_CENTRAL_1)
          .build()

    val store = storeForKV(new DynamoDB(client))
    val table = store.table[SomeClass]("sometable")
    
    val entries = for { 
      _ <- table.put("PK", 10000)(SomeClass("value",2)))
      _ <- table.put("PK", 20000)(SomeClass("value",2)))
    } yield table.query("PK",Some(0), Some(30000))

    Await.result(entries)

