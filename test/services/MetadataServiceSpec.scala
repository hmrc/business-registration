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

package services

import fixtures.{MetadataFixture, MongoFixture}
import models.Metadata
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any, contains, eq => eqTo}
import org.scalactic.source.Position
import org.scalatest.Tag
import org.scalatest.mockito.MockitoSugar
import repositories._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class MetadataServiceSpec extends UnitSpec with MockitoSugar with MetadataFixture with MongoFixture {

  implicit val mongo = mongoDB

  val mockMetadataRepository = mock[MetadataRepositoryMongo]
  val mockMetadataMongo = mock[MetadataMongo]
  val mockSequenceRepository = mock[SequenceRepository]

  def setupService: MetadataService = {
    new MetadataService(mockMetadataMongo, mockSequenceRepository){
      override val metadataRepository = mockMetadataRepository
    }
  }

  "calling createMetadataRecord" should {

    val service = setupService

    "create a new metadata document" in {
      when(mockMetadataRepository.createMetadata(any[Metadata]())(any()))
        .thenReturn(Future.successful(buildMetadata()))
      when(mockSequenceRepository.getNext(contains("registrationID"))(any()))
        .thenReturn(Future.successful(1))

      val result = service.createMetadataRecord("intId", "en")
      await(result) shouldBe buildMetadataResponse()
    }
  }

  "retrieveMetadataRecord" should {

    val service = setupService

    "return MetadataResponse when a metadata document is retrieved" in {
      when(mockMetadataRepository.retrieveMetadata(any())(any()))
        .thenReturn(Future.successful(Some(buildMetadata())))

      val result = service.retrieveMetadataRecord("testRegID")
      await(result) shouldBe Some(buildMetadataResponse())
    }

    "return None if no document is retrieved" in {
      when(mockMetadataRepository.retrieveMetadata(any())(any()))
        .thenReturn(Future.successful(None))

      val result = service.retrieveMetadataRecord("testRegID")
      await(result) shouldBe None
    }
  }
  "searchMetadataRecord" should {

    val service = setupService

    "return MetadataResponse when a metadata document is retrieved" in {
      when(mockMetadataRepository.searchMetadata(any())(any()))
        .thenReturn(Future.successful(Some(buildMetadata())))
      val result = service.searchMetadataRecord("testIntID")
      await(result) shouldBe Some(buildMetadataResponse())
    }

    "return None if no document is retrieved" in {
      when(mockMetadataRepository.searchMetadata(any())(any()))
        .thenReturn(Future.successful(None))

      val result = service.searchMetadataRecord("testIntID")
      await(result) shouldBe None
    }
  }

  "updateMetaDataRecord" should {

    val service = setupService

    "return a meta data response" in {
      when(mockMetadataRepository.updateMetaData(eqTo("testIntID"), eqTo(buildMetadataResponse()))(any()))
        .thenReturn(Future.successful(buildMetadataResponse()))

      val result = service.updateMetaDataRecord("testIntID", buildMetadataResponse())
      await(result) shouldBe buildMetadataResponse()
    }
  }

  "removeMetaDataRecord" should {

    val service = setupService

    "return a Boolean" in {
      when(mockMetadataRepository.removeMetadata(eqTo("testIntID"))(any()))
        .thenReturn(Future.successful(true))

      val result = service.removeMetadata("testIntID")
      await(result) shouldBe true
    }
  }
  "update last signed in" should {
    val service = setupService
    val currentTime = DateTime.now(DateTimeZone.UTC)
    "return a date time response" in {
      when(mockMetadataRepository.updateLastSignedIn(eqTo("testIntID"), eqTo(currentTime))(any()))
        .thenReturn(Future.successful(currentTime))

      val result = service.updateLastSignedIn("testIntID", currentTime)
      await(result) shouldBe currentTime
    }
  }
}
