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

package repositories

import models.{Metadata, MetadataResponse}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import play.api.test.Helpers._
import repositories.MetadataMongoRepository
import helpers.MongoSpec

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import scala.language.postfixOps

class MetadataMongoRepositoryISpec extends MongoSpec with BeforeAndAfterAll with ScalaFutures with Eventually {

  class Setup {

    val repository: MetadataMongoRepository = app.injector.instanceOf[MetadataMongoRepository]
    val randomIntId: String = UUID.randomUUID().toString
    val randomRegid: String = UUID.randomUUID().toString
    val metadata: Metadata = Metadata(randomIntId, randomRegid, "", "ENG", None, None, declareAccurateAndComplete = false)

    def createMetadata(data: Metadata = metadata): Metadata = await(repository.createMetadata(data))

    def retrieveMetadata(regId: String = randomRegid): Option[Metadata] = await(repository.retrieveMetadata(regId))

    def searchMetadata(intId: String = randomIntId): Option[Metadata] = await(repository.searchMetadata(intId))

    def getInternalId(regId: String = randomRegid): Option[String] = await(repository.getInternalId(regId))

    def updateMetatdata(regId: String = randomRegid, data: MetadataResponse): MetadataResponse = await(repository.updateMetaData(regId, data))

    def removeMetadata(regId: String = randomRegid): Boolean = await(repository.removeMetadata(regId))

    def updateCompletionCapacity(regId: String = randomRegid, cc: String): String = await(repository.updateCompletionCapacity(regId, cc))

    def updateLastSignin(regId: String = randomRegid, timeNow: Instant): Instant = await(repository.updateLastSignedIn(regId, timeNow))

    def count: Int = repository.awaitCount

    repository.removeAll()
    await(repository.ensureIndexes)
  }

  "MetadataRepository" should {

    "be able to retrieve a document that has been created by internalID" in new Setup {
      val metdataResponse: Metadata = createMetadata()

      metdataResponse.internalId mustBe randomIntId

      val mdByIntId: Option[Metadata] = searchMetadata()

      mdByIntId mustBe defined
      mdByIntId.get.internalId mustBe randomIntId
      mdByIntId.get.registrationID mustBe randomRegid
    }

    "be able to retrieve a document that has been created by registration id" in new Setup {

      val metdataResponse: Metadata = createMetadata()

      metdataResponse.registrationID mustBe randomRegid

      val mdByRegId: Option[Metadata] = retrieveMetadata()

      mdByRegId mustBe defined
      mdByRegId.get.internalId mustBe randomIntId
      mdByRegId.get.registrationID mustBe randomRegid
    }

    "be able to use the authorisation call to check a document" in new Setup {

      val metdataResponse: Metadata = createMetadata()

      metdataResponse.registrationID mustBe randomRegid

      val auth: Option[String] = getInternalId()

      auth mustBe defined
      auth mustBe Some(randomIntId)
    }

    "return None for the authorisation call when there's no document" in new Setup {
      val auth: Option[String] = getInternalId()
      auth mustBe None
    }

    "Update Last Sign In" should {
      "update the last sign in date if record exists" in new Setup {

        createMetadata()
        val timeNow: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)

        val updateResponse: Instant = updateLastSignin(timeNow = timeNow)

        updateResponse mustBe timeNow

        val queryResponse: Option[Metadata] = retrieveMetadata()

        queryResponse mustBe defined
        queryResponse.get.lastSignedIn mustBe timeNow
      }
    }

    "UpdateMetData" should {
      "update the Completion Capacity if record is present" in new Setup {
        val metadataResponse: Metadata = createMetadata()
        val newMetadataResponseModel: MetadataResponse = MetadataResponse(
          registrationID = randomRegid,
          formCreationTimestamp = metadataResponse.formCreationTimestamp,
          language = metadataResponse.language,
          completionCapacity = Some("newCapacity"))

        val updateResponse: MetadataResponse = updateMetatdata(data = newMetadataResponseModel)

        updateResponse mustBe newMetadataResponseModel

        val queryResponse: Option[Metadata] = retrieveMetadata()

        queryResponse mustBe defined
        queryResponse.get.completionCapacity mustBe Some("newCapacity")
      }
    }

    "removeMetadata" should {
      "remove the required record" in new Setup {
        val metadataResponse: Metadata = createMetadata()
        count mustBe 1

        val removeResponse: Boolean = removeMetadata()
        val queryResponse: Option[Metadata] = retrieveMetadata()

        queryResponse mustBe None
        count mustBe 0
      }
    }

    "updateCompletionCapacity" should {
      "successfully update the completion capacity in a document" in new Setup {
        val metdataResponse: Metadata = createMetadata()

        val updatedCapacity: String = updateCompletionCapacity(cc = "director")
        updatedCapacity mustBe "director"

        val fetchedMetaData: Option[Metadata] = retrieveMetadata()
        fetchedMetaData.get.completionCapacity mustBe Some("director")
      }
    }
  }
}