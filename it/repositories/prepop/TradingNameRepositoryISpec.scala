/*
 * Copyright 2020 HM Revenue & Customs
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

package repositories.prepop

import models.prepop.PermissionDenied
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONElement, BSONLong}
import uk.gov.hmrc.mongo.{MongoSpecSupport, ReactiveRepository}

import scala.concurrent.ExecutionContext.Implicits.global

class TradingNameRepositoryISpec extends PlaySpec with MongoSpecSupport with BeforeAndAfterAll
  with ScalaFutures with Eventually with GuiceOneAppPerSuite {


  val timeToExpire: Int = 9999

  val additionalConfig = Map("Test.microservice.services.prePop.ttl" -> s"$timeToExpire")

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(additionalConfig)
    .build()

  class Setup {

    val repository: TradingNameRepository = app.injector.instanceOf[TradingNameRepository]
    await(repository.removeAll())
    await(repository.ensureIndexes)

    def indexCount: Int = await(repository.collection.indexesManager.list).size

    def count: Int = await(repository.count)
  }

  "upsertTradingName" should {
    "add the trading name if none exists" in new Setup {
      count mustBe 0
      await(repository.upsertTradingName("regId", "intId", "my trading name"))
      count mustBe 1
    }

    "return Forbidden if user is not authorised" in new Setup {
      count mustBe 0
      await(repository.upsertTradingName("regId", "intId", "my trading name"))
      count mustBe 1
      intercept[PermissionDenied](await(repository.upsertTradingName("regId", "wrongIntId", "my trading name")))
    }

    "update trading name if it exists" in new Setup {
      count mustBe 0
      await(repository.upsertTradingName("regId", "intId", "my trading name"))
      count mustBe 1
      val res: Option[String] = await(repository.upsertTradingName("regId", "intId", "my new trading name"))
      count mustBe 1
      res mustBe Some("my new trading name")
    }
  }

  "getTradingName" should {
    "return none if nothing exists" in new Setup {
      count mustBe 0
      await(repository.getTradingName("regId", "intId")) mustBe None
    }

    "return the trading name if one exists" in new Setup {
      count mustBe 0
      await(repository.upsertTradingName("regId", "intId", "my trading name"))
      count mustBe 1
      await(repository.getTradingName("regId", "intId")) mustBe Some("my trading name")
    }

    "return Forbidden if user is not authorised" in new Setup {
      count mustBe 0
      await(repository.upsertTradingName("regId", "intId", "my trading name"))
      count mustBe 1
      intercept[PermissionDenied](await(repository.getTradingName("regId", "wrongIntId")))
    }
  }

  "ttl index" should {

    def getTTLIndex(repo: ReactiveRepository[_, _]): Index = {
      await(repo.collection.indexesManager.list).filter(_.eventualName == "lastUpdatedIndex").head
    }

    "be applied with the correct expiration if one doesn't already exist" in new Setup {

      await(repository.collection.indexesManager.dropAll())

      indexCount mustBe 1 // default _id index

      await(repository.ensureIndexes)

      indexCount mustBe 2

      val indexes: List[Index] = await(repository.collection.indexesManager.list)

      indexes.exists(_.eventualName == "lastUpdatedIndex") mustBe true
      getTTLIndex(repository).options.elements mustBe Stream(BSONElement("expireAfterSeconds", BSONLong(timeToExpire)))
    }

    "overwrite the current ttl index with a new one from config" in new Setup {

      await(repository.collection.indexesManager.dropAll())

      indexCount mustBe 1 // default _id index

      await(repository.ensureIndexes)

      indexCount mustBe 2

      await(repository.collection.indexesManager.drop("lastUpdatedIndex"))

      indexCount mustBe 1 // dropped ttl index

      val setupTTl: Int = 1

      val setupTTLIndex: Index = Index(
        key = Seq("lastUpdated" -> IndexType.Ascending),
        name = Some("lastUpdatedIndex"),
        options = BSONDocument("expireAfterSeconds" -> BSONLong(setupTTl))
      )

      await(repository.collection.indexesManager.create(setupTTLIndex))

      indexCount mustBe 2 // created new ttl index

      val indexes: List[Index] = await(repository.collection.indexesManager.list)

      indexes.exists(_.eventualName == "lastUpdatedIndex") mustBe true
      getTTLIndex(repository).options.elements mustBe Stream(BSONElement("expireAfterSeconds", BSONLong(setupTTl)))

      await(repository.ensureIndexes)

      indexCount mustBe 2

      indexes.exists(_.eventualName == "lastUpdatedIndex") mustBe true
      getTTLIndex(repository).options.elements mustBe Stream(BSONElement("expireAfterSeconds", BSONLong(timeToExpire)))
    }

    "do nothing when ensuring the ttl index but one already exists and has the same expiration time" in new Setup {
      await(repository.collection.indexesManager.dropAll())

      indexCount mustBe 1 // default _id index

      await(repository.ensureIndexes)

      indexCount mustBe 2

      val indexes: List[Index] = await(repository.collection.indexesManager.list)

      indexes.exists(_.eventualName == "lastUpdatedIndex") mustBe true
      getTTLIndex(repository).options.elements mustBe Stream(BSONElement("expireAfterSeconds", BSONLong(timeToExpire)))

      await(repository.ensureIndexes)

      indexes.exists(_.eventualName == "lastUpdatedIndex") mustBe true
      getTTLIndex(repository).options.elements mustBe Stream(BSONElement("expireAfterSeconds", BSONLong(timeToExpire)))
    }
  }

}
