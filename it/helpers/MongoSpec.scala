/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package helpers

import org.mongodb.scala.bson.{BsonDocument, ObjectId}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.result.{DeleteResult, InsertOneResult}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.MongoSupport

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag
import scala.util.Random

trait MongoSpec extends PlaySpec with MongoSupport with GuiceOneAppPerSuite with RichMongoRepository {
  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def generateOID: String = {
    val alpha = "abcdef123456789"
    (1 to 24).map(x => alpha(Random.nextInt.abs % alpha.length)).mkString
  }
}

trait RichMongoRepository {
  self: PlaySpec =>

  import scala.language.implicitConversions

  implicit class MongoOps[T](repo: PlayMongoRepository[T])(implicit ec: ExecutionContext) {
    def removeAll(): DeleteResult = await(repo.collection.deleteMany(BsonDocument()).toFuture())
    def awaitCount: Int = await(repo.collection.countDocuments().toFuture()).toInt
    def awaitInsert(e: T): InsertOneResult = await(repo.collection.insertOne(e).toFuture())
    def findById(id: ObjectId)(implicit ct: ClassTag[T]): Option[T] = await(repo.collection.find(Filters.equal("_id", id)).headOption())
  }

  implicit class JsObjectHelpers(o: JsObject) {
    def pretty: String = Json.prettyPrint(o)
  }

  implicit def toJsObject(v: JsValue): JsObject = v.as[JsObject]

  case class Index(name: String, expireAfterSeconds: Option[Int])

  object Index {
    implicit val format = Json.format[Index]
  }

  def getTTLIndex(repo: PlayMongoRepository[_]): Index = {
    val documents = await(repo.collection.listIndexes.toFuture())
    val indexesJson = documents.map(doc => Json.parse(doc.toJson()))
    indexesJson.map(_.as[Index]).find(_.name == "lastUpdatedIndex").get
  }
}
