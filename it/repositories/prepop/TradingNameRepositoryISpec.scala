/*
 * Copyright 2022 HM Revenue & Customs
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

import helpers.MongoSpec
import models.prepop.PermissionDenied
import org.mockito.Mockito.when
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.concurrent.TimeUnit

class TradingNameRepositoryISpec extends MongoSpec with BeforeAndAfterAll with ScalaFutures with Eventually with MockitoSugar {

  class Setup(allowReplaceTimeToLiveIndex: Boolean = true) {

    val timeToExpire: Int = 9999
    val mockServicesConfig = mock[ServicesConfig]

    when(mockServicesConfig.getInt("microservice.services.prePop.ttl")).thenReturn(timeToExpire)
    when(mockServicesConfig.getBoolean("mongodb.allowReplaceTimeToLiveIndex")).thenReturn(allowReplaceTimeToLiveIndex)

    val repository: TradingNameRepository = new TradingNameRepository(mongoComponent, mockServicesConfig)

    repository.removeAll()
    await(repository.ensureIndexes)

    def indexCount: Int = await(repository.collection.listIndexes().toFuture()).size

    def count: Int = repository.awaitCount
  }

  "upsertTradingName" should {
    "add the trading name if none exists" in new Setup() {
      count mustBe 0
      await(repository.upsertTradingName("regId", "intId", "my trading name"))
      count mustBe 1
    }

    "return Forbidden if user is not authorised" in new Setup() {
      count mustBe 0
      await(repository.upsertTradingName("regId", "intId", "my trading name"))
      count mustBe 1
      intercept[PermissionDenied](await(repository.upsertTradingName("regId", "wrongIntId", "my trading name")))
    }

    "update trading name if it exists" in new Setup() {
      count mustBe 0
      await(repository.upsertTradingName("regId", "intId", "my trading name"))
      count mustBe 1
      val res: Option[String] = await(repository.upsertTradingName("regId", "intId", "my new trading name"))
      count mustBe 1
      res mustBe Some("my new trading name")
    }
  }

  "getTradingName" should {
    "return none if nothing exists" in new Setup() {
      count mustBe 0
      await(repository.getTradingName("regId", "intId")) mustBe None
    }

    "return the trading name if one exists" in new Setup() {
      count mustBe 0
      await(repository.upsertTradingName("regId", "intId", "my trading name"))
      count mustBe 1
      await(repository.getTradingName("regId", "intId")) mustBe Some("my trading name")
    }

    "return Forbidden if user is not authorised" in new Setup() {
      count mustBe 0
      await(repository.upsertTradingName("regId", "intId", "my trading name"))
      count mustBe 1
      intercept[PermissionDenied](await(repository.getTradingName("regId", "wrongIntId")))
    }
  }

  "ttl index" should {

    "be applied with the correct expiration if one doesn't already exist" in new Setup() {

      await(repository.collection.dropIndexes().toFuture())

      indexCount mustBe 1 // default _id index

      await(repository.ensureIndexes)

      indexCount mustBe 2

      getTTLIndex(repository).expireAfterSeconds mustBe Some(timeToExpire)
    }

    "overwrite the current ttl index with a new one from config" in new Setup() {

      await(repository.collection.dropIndexes().toFuture())

      indexCount mustBe 1 // default _id index

      await(repository.ensureIndexes)

      indexCount mustBe 2

      await(repository.collection.dropIndex("lastUpdatedIndex").toFuture())

      indexCount mustBe 1 // dropped ttl index

      val setupTTl: Int = 1

      val setupTTLIndex: IndexModel = IndexModel(
        ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIndex")
          .expireAfter(setupTTl, TimeUnit.SECONDS)
      )

      await(repository.collection.createIndexes(Seq(setupTTLIndex)).toFuture())

      indexCount mustBe 2 // created new ttl index

      getTTLIndex(repository).expireAfterSeconds mustBe Some(setupTTl)

      await(repository.ensureIndexes)

      indexCount mustBe 2

      getTTLIndex(repository).expireAfterSeconds mustBe Some(timeToExpire)
    }

    "do nothing when ensuring the ttl index but one already exists and has the same expiration time" in new Setup() {
      await(repository.collection.dropIndexes().toFuture())

      indexCount mustBe 1 // default _id index

      await(repository.ensureIndexes)

      indexCount mustBe 2

      getTTLIndex(repository).expireAfterSeconds mustBe Some(timeToExpire)

      await(repository.ensureIndexes)

      getTTLIndex(repository).expireAfterSeconds mustBe Some(timeToExpire)
    }
  }

}
