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

import models.{Metadata, MetadataResponse}
import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

class MetadataMongoRepositoryISpec extends UnitSpec with MongoSpecSupport with BeforeAndAfterAll with ScalaFutures with Eventually with WithFakeApplication {

  //implicit val app = new GuiceApplicationBuilder().build()
  implicit val defaultEC: ExecutionContext = ExecutionContext.global.prepare()

  class Setup {
    val mongoComp       = fakeApplication.injector.instanceOf[ReactiveMongoComponent]
    val repository      = new MetadataMongo(mongoComp).repository
    val randomIntId     = UUID.randomUUID().toString
    val randomRegid     = UUID.randomUUID().toString
    val metadata        = Metadata(randomIntId, randomRegid, "", "ENG", None, None, false)

    def createMetadata(data: Metadata = metadata)                             = await(repository.createMetadata(data))
    def retrieveMetadata(regId: String = randomRegid)                         = await(repository.retrieveMetadata(regId))
    def searchMetadata(intId: String = randomIntId)                           = await(repository.searchMetadata(intId))
    def getInternalId(regId: String = randomRegid)                            = await(repository.getInternalId(regId))
    def updateMetatdata(regId: String = randomRegid, data: MetadataResponse)  = await(repository.updateMetaData(regId, data))
    def removeMetadata(regId: String = randomRegid)                           = await(repository.removeMetadata(regId))
    def updateCompletionCapacity(regId: String = randomRegid, cc: String)     = await(repository.updateCompletionCapacity(regId, cc))
    def updateLastSignin(regId: String = randomRegid, timeNow: DateTime)      = await(repository.updateLastSignedIn(regId, timeNow))
    def count                                                                 = await(repository.count)

    await(repository.drop)
    await(repository.ensureIndexes)
  }

  override def afterAll() = new Setup {
    await(repository.drop)
  }

  "MetadataRepository" should {

    "be able to retrieve a document that has been created by internalID" in new Setup {
      val metdataResponse = createMetadata()

      metdataResponse.internalId shouldBe randomIntId

      val mdByIntId = searchMetadata()

      mdByIntId shouldBe defined
      mdByIntId.get.internalId shouldBe randomIntId
      mdByIntId.get.registrationID shouldBe randomRegid
    }

    "be able to retrieve a document that has been created by registration id" in new Setup {

      val metdataResponse = createMetadata()

      metdataResponse.registrationID shouldBe randomRegid

      val mdByRegId = retrieveMetadata()

      mdByRegId shouldBe defined
      mdByRegId.get.internalId shouldBe randomIntId
      mdByRegId.get.registrationID shouldBe randomRegid
    }

    "be able to use the authorisation call to check a document" in new Setup {

      val metdataResponse = createMetadata()

      metdataResponse.registrationID shouldBe randomRegid

      val auth = getInternalId()

      auth shouldBe defined
      auth shouldBe Some(randomIntId)
    }

    "return None for the authorisation call when there's no document" in new Setup {
      val auth = getInternalId()
      auth shouldBe None
    }
  }

  "Update Last Sign In" should {
    "update the last sign in date if record exists" in new Setup {
      val metdataResponse = createMetadata()
      val timeNow = DateTime.now()

      val updateResponse = updateLastSignin(timeNow = timeNow)

      updateResponse shouldBe timeNow

      val queryResponse = retrieveMetadata()

      queryResponse shouldBe defined
      queryResponse.get.lastSignedIn shouldBe timeNow
    }
  }

  "UpdateMetData" should {
    "update the Completion Capacity if record is present" in new Setup {
      val metadataResponse          = createMetadata()
      val newMetadataResponseModel  = MetadataResponse(
        registrationID        = randomRegid,
        formCreationTimestamp = metadataResponse.formCreationTimestamp,
        language              = metadataResponse.language,
        completionCapacity    = Some("newCapacity"))

      val updateResponse = updateMetatdata(data = newMetadataResponseModel)

      updateResponse shouldBe newMetadataResponseModel

      val queryResponse = retrieveMetadata()

      queryResponse shouldBe defined
      queryResponse.get.completionCapacity shouldBe Some("newCapacity")
    }
  }

  "removeMetadata" should {
    "remove the required record" in new Setup {
      val metadataResponse = createMetadata()
      count shouldBe 1

      val removeResponse = removeMetadata()
      val queryResponse = retrieveMetadata()

      queryResponse shouldBe None
      count shouldBe 0
    }
  }

  "updateCompletionCapacity" should {
    "successfully update the completion capacity in a document" in new Setup {
      val metdataResponse = createMetadata()

      val updatedCapacity = updateCompletionCapacity(cc = "director")
      updatedCapacity shouldBe "director"

      val fetchedMetaData = retrieveMetadata()
      fetchedMetaData.get.completionCapacity shouldBe Some("director")
    }
  }
}