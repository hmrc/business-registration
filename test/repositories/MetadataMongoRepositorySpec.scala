/*
 * Copyright 2017 HM Revenue & Customs
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

import fixtures.MetadataFixture
import helpers.{MongoMocks, SCRSSpec}
import models.Metadata
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mockito.MockitoSugar
import reactivemongo.bson.{BSONDocument, BSONString}
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class MetadataMongoRepositorySpec extends UnitSpec with MockitoSugar with MongoMocks with BeforeAndAfter with MetadataFixture {

  val repository = new MetadataRepositoryMongo(() => mockMongoDb){
    override lazy val collection = mockCollection(Some("test"))
  }

  before {
    reset(repository.collection)
  }

  "MetadataMongoRepository search by internalId" should {

    val randomInternalId = UUID.randomUUID().toString
    val randomRegid = UUID.randomUUID().toString

    "Find a document keyed on internalID when one exists" in {

      val metadataModel = mock[Metadata]

      when(metadataModel.registrationID) thenReturn randomRegid

      val selector = BSONDocument("internalId" -> BSONString(randomInternalId))
      setupFindFor(repository.collection, selector, Some(metadataModel))

      val result = await(repository.searchMetadata(randomInternalId))

      result should be(defined)
      result.get should be(metadataModel)

      result match {
        case Some(m) => m.registrationID shouldBe randomRegid
        case None => fail("Expected a response, got None")
      }
    }
  }

  "MetadataMongoRepository retrieve by registration id" should {

    val randomInternalId = UUID.randomUUID().toString
    val randomRegid = UUID.randomUUID().toString

    "Find a document keyed on registration id when one exists" in {

      val metadataModel = mock[Metadata]

      when(metadataModel.registrationID) thenReturn randomRegid

      val selector = BSONDocument("registrationID" -> BSONString(randomRegid))
      setupFindFor(repository.collection, selector, Some(metadataModel))

      val result = await(repository.retrieveMetadata(randomRegid))

      result should be(defined)
      result.get should be(metadataModel)

      result match {
        case Some(m) => m.registrationID shouldBe randomRegid
        case None => fail("Expected a response, got None")
      }
    }
  }

  "MetadataMongoRepository create metadata" should {
    val randomInternalId = UUID.randomUUID().toString
    val randomRegid = UUID.randomUUID().toString

    "Store a document " in {

      val captor = ArgumentCaptor.forClass[Metadata, Metadata](classOf[Metadata])

      val metadata = Metadata(randomInternalId, randomRegid, "", "en", None, None, false)

      setupAnyInsertOn(repository.collection, fails = false)

      val metadataResult = await(repository.createMetadata(metadata))

      verifyInsertOn(repository.collection, captor)

      captor.getValue.internalId shouldBe randomInternalId
      captor.getValue.registrationID shouldBe randomRegid

      metadataResult.internalId shouldBe randomInternalId
      metadataResult.registrationID shouldBe randomRegid
    }
  }

  "MetadataMongoRepository retrieve by id for authorisation" should {

    val randomInternalID = UUID.randomUUID().toString
    val randomRegid = UUID.randomUUID().toString

    "Find a document keyed on registration id when one exists" in {

      val metadataModel = mock[Metadata]

      when(metadataModel.registrationID) thenReturn randomRegid
      when(metadataModel.internalId) thenReturn randomInternalID

      val selector = BSONDocument("registrationID" -> BSONString(randomRegid))
      setupFindFor(repository.collection, selector, Some(metadataModel))

      val result = await(repository.getInternalId(randomRegid))

      result should be(defined)
      result should be(Some((randomRegid, randomInternalID)))
    }

    "return None when no document exists" in {

      val metadataModel = mock[Metadata]

      when(metadataModel.registrationID) thenReturn randomRegid
      when(metadataModel.internalId) thenReturn randomInternalID

      val selector = BSONDocument("registrationID" -> BSONString(randomRegid))
      setupFindFor(repository.collection, selector, None)

      val result = await(repository.getInternalId(randomRegid))

      result should be(None)
    }
  }

  "MetaDataMongoRepo update cc data" should {
    "return a metadataresponse" in {
      val selector = BSONDocument("registrationID" -> "testRegID")

      setupAnyUpdateOn(repository.collection)

      val result = repository.updateMetaData("testRegID", buildMetadataResponse())

      await(result) shouldBe buildMetadataResponse()
    }
  }
}
