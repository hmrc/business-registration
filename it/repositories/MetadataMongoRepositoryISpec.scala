/*
 * Copyright 2016 HM Revenue & Customs
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

package repositories

import java.util.UUID

import models.Metadata
import org.scalatest._
import org.scalatest.fixture.NoArg
import scala.concurrent.duration._
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import play.api.inject.guice.GuiceApplicationBuilder
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.api.DefaultDB
import reactivemongo.json.collection.JSONCollection
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.mongo.MongoSpecSupport
import play.api.test.{FakeApplication, Helpers}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class MetadataMongoRepositoryISpec extends UnitSpec with MongoSpecSupport with BeforeAndAfter with ScalaFutures with Eventually {
  //this: TestSuite =>

  def setupRepo = new MetadataMongoRepository()

  class Setup {
    val repository = new MetadataMongoRepository()
    await(repository.drop)
    await(repository.ensureIndexes)
  }

  after {
    await(setupRepo.drop)
  }

  implicit final def app = new FakeApplication()
  val timeout = 10 seconds

  override def withFixture(test: Nothing) = {
    Helpers.running(app) {
      val collection = mongo().collection[JSONCollection]("collectionName")
      Await.ready(collection.drop(), timeout)
      super.withFixture(test)
    }
  }

  "MetadataRepository" should {

    "be able to retrieve a document that has been created by internalID" in new Setup {

      val randomIntId = UUID.randomUUID().toString
      val randomRegid = UUID.randomUUID().toString

      val metadata = Metadata(randomIntId, randomRegid, "", "ENG", None, None, false)

      val metdataResponse = await(repository.createMetadata(metadata))

      metdataResponse.internalId shouldBe randomIntId

      val mdByIntId = await(repository.searchMetadata(randomIntId))

      mdByIntId shouldBe (defined)
      mdByIntId.get.internalId shouldBe (randomIntId)
      mdByIntId.get.registrationID shouldBe (randomRegid)
    }

    "be able to retrieve a document that has been created by registration id" in new Setup {

      val randomIntId = UUID.randomUUID().toString
      val randomRegid = UUID.randomUUID().toString

      val metadata = Metadata(randomIntId, randomRegid, "", "ENG", None, None, false)

      val metdataResponse = await(repository.createMetadata(metadata))

      metdataResponse.registrationID shouldBe (randomRegid)

      val mdByRegId = await(repository.retrieveMetadata(randomRegid))

      mdByRegId shouldBe (defined)
      mdByRegId.get.internalId shouldBe (randomIntId)
      mdByRegId.get.registrationID shouldBe (randomRegid)
    }

    "be able to use the authorisation call to check a document" in new Setup {

      val randomIntId = UUID.randomUUID().toString
      val randomRegid = UUID.randomUUID().toString

      val metadata = Metadata(randomIntId, randomRegid, "", "ENG", None, None, false)

      val metdataResponse = await(repository.createMetadata(metadata))

      metdataResponse.registrationID shouldBe (randomRegid)

      val auth = await(repository.getInternalId(randomRegid))

      auth shouldBe (defined)
      auth shouldBe Some((randomRegid, randomIntId))
    }

    "return None for the authorisation call when there's no document" in new Setup {
      val randomRegid = UUID.randomUUID().toString
      val auth = await(repository.getInternalId(randomRegid))
      auth shouldBe None
    }
  }
}