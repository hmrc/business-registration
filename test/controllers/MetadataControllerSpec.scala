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

package controllers

import akka.actor.ActorSystem
import akka.stream.{Materializer, ActorMaterializer}
import connectors.{Authority, AuthConnector}
import fixtures.{AuthFixture, MetadataFixture}
import helpers.AuthMocks
import mocks.MetricServiceMock
import models.ErrorResponse
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.MetadataMongoRepository
import services.MetadataService
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.ArgumentCaptor
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class MetadataControllerSpec extends UnitSpec with MetadataFixture with AuthFixture with MockitoSugar with AuthMocks {

  val mockMetadataService = mock[MetadataService]
  val mockMetadataRepo = mock[MetadataMongoRepository]
  implicit val mockAuthConnector = mock[AuthConnector]

  //todo add materializer to SCRSSpec as the underlying Action.run() call in play.api.test.Helpers.call() needs it
  //todo find out if it can be found elsewhere instead of creating new one here
  //todo speak to CJWalks about using .run() to force Future[Results] from Actions - where does he get implicit Materializer from?
  implicit val actorSystem = ActorSystem()
  implicit val materializer: Materializer = ActorMaterializer()

//  override protected def beforeEach() = {
//    reset(
//      mockMetadataService,
//      mockAuthConnector,
//      mockMetadataRepo,
//      MetricServiceMock
//    )
//  }

  def setupController(authority: Option[Authority]): MetadataController = {
    mockGetCurrentAuthority(authority)
    new MetadataController(mockMetadataService, mockAuthConnector, MetricServiceMock, mockMetadataRepo)
  }

  "calling createMetadata" when {

    "the service returns a newly created metadata entry" should {
      val controller = setupController(Some(validAuthority))

      when(mockMetadataService.createMetadataRecord(any(), any()))
        .thenReturn(Future.successful(buildMetadataResponse()))

      val metadataRequest = buildMetadataRequest()
      val request = FakeRequest().withJsonBody(Json.toJson(metadataRequest))
      val result = call(controller.createMetadata, request)

      "return a 201" in {
        status(result) shouldBe CREATED
      }

      "return a response which contains a valid metadata response as json" in {
        contentType(result) shouldBe Some("application/json")
        await(jsonBodyOf(result)).as[JsObject] shouldBe metadataResponseJsObj
      }
    }

    "the user is not authenticated" should {
      val controller = setupController(None)
      val request = FakeRequest().withJsonBody(validMetadataJson)
      val result = call(controller.createMetadata, request)

      "return a 403" in {
        status(result) shouldBe FORBIDDEN
      }
    }
  }

  "searchMetadata" should {

    "return a 200 and a MetadataResponse as json if metadata is found" in {
      val controller = setupController(Some(validAuthority))

      when(mockMetadataService.searchMetadataRecord(any()))
        .thenReturn(Future.successful(Some(buildMetadataResponse())))
      //MetadataServiceMocks.searchMetadataRecord(validAuthority.ids.internalId, Some(buildMetadataResponse()))

      val result = call(controller.searchMetadata, FakeRequest())
      status(result) shouldBe OK
      await(jsonBodyOf(result)).as[JsObject] shouldBe metadataResponseJsObj
    }

    "return a 403 - forbidden when the user is not authenticated" in {
      val controller = setupController(None)

      val result = call(controller.searchMetadata, FakeRequest())
      status(result) shouldBe FORBIDDEN
    }

    "return a 404 - NotFound when the resource doesn't exist" in {
      val controller = setupController(Some(validAuthority))

      //MetadataServiceMocks.searchMetadataRecord(validAuthority.ids.internalId, None)
      when(mockMetadataService.searchMetadataRecord(any()))
        .thenReturn(Future.successful(None))

      val result = call(controller.searchMetadata, FakeRequest())
      status(result) shouldBe NOT_FOUND
      await(jsonBodyOf(result)) shouldBe ErrorResponse.MetadataNotFound
    }
  }

  "retrieveMetadata" should {

    val regId = "0123456789"

    "return a 200 and a metadata model is one is found" in {
      val controller = setupController(Some(validAuthority))

      val regIdCaptor = ArgumentCaptor.forClass[String, String](classOf[String])

      when(mockMetadataService.retrieveMetadataRecord(regIdCaptor.capture()))
        .thenReturn(Future.successful(Some(buildMetadataResponse())))

      val result = call(controller.retrieveMetadata(regId), FakeRequest())

      status(result) shouldBe OK
      regIdCaptor.getValue shouldBe regId
      await(jsonBodyOf(result)).as[JsValue] shouldBe metadataResponseJsObj
    }

    "return a 403 when the user is not logged in" in {
      val controller = setupController(Some(validAuthority))

      val regIdCaptor = ArgumentCaptor.forClass[String, String](classOf[String])

      when(mockMetadataService.retrieveMetadataRecord(regIdCaptor.capture()))
        .thenReturn(Future.successful(Some(buildMetadataResponse())))

      val result = call(controller.retrieveMetadata(regId), FakeRequest())
      status(result) shouldBe FORBIDDEN

      verify(mockMetadataService, times(0)).retrieveMetadataRecord(any())
    }

    "return a 403 when the user is logged in but not authorised to access the resource" in {
      val controller = setupController(Some(validAuthority))

      val result = call(controller.retrieveMetadata(regId), FakeRequest())
      status(result) shouldBe FORBIDDEN
    }

    "return a 404 when the user is logged in but the requested document doesn't exist" in {
      val controller = setupController(Some(validAuthority))

      val result = call(controller.retrieveMetadata(regId), FakeRequest())
      status(result) shouldBe NOT_FOUND
    }

    "return a 404 - not found logged in the requested document doesn't exist but got through auth" in {
      val controller = setupController(Some(validAuthority))

      when(mockMetadataRepo.getInternalId(eqTo(regId))).
        thenReturn(Future.successful(Some((regId,validAuthority.ids.internalId))))
      //MetadataServiceMocks.retrieveMetadataRecord(regId, None)
      when(mockMetadataService.retrieveMetadataRecord(eqTo(regId)))
        .thenReturn(Future.successful(None))

      val result = call(controller.retrieveMetadata(regId), FakeRequest())
      status(result) shouldBe NOT_FOUND
      await(jsonBodyOf(result)) shouldBe ErrorResponse.MetadataNotFound
    }
  }

  "updateMetaData" should {

    val regId = "0123456789"

    "return a 200 if Json body can be parsed and meta data has been updated" in {
      val controller = setupController(Some(validAuthority))
      mockSuccessfulAuthorisation(mockMetadataRepo, regId, validAuthority)

      when(mockMetadataService.updateMetaDataRecord(eqTo(regId), eqTo(buildMetadataResponse())))
        .thenReturn(Future.successful(buildMetadataResponse()))

      val result = call(controller.updateMetaData(regId), FakeRequest().withJsonBody(Json.toJson(buildMetadataResponse())))
      status(result) shouldBe OK
      await(jsonBodyOf(result)).as[JsObject] shouldBe metadataResponseJsObj
    }

    "return a 403 when the user is not logged in" in {
      val controller = setupController(None)
      mockNotLoggedIn(mockMetadataRepo)

      val result = call(controller.updateMetaData(regId), FakeRequest().withJsonBody(Json.toJson(buildMetadataResponse())))
      status(result) shouldBe FORBIDDEN
    }

    "return a 403 when the user is logged in but not authorised to access the resource" in {
      val controller = setupController(Some(validAuthority))
      mockNotAuthorised(mockMetadataRepo, regId, validAuthority)

      val result = call(controller.updateMetaData(regId), FakeRequest().withJsonBody(Json.toJson(buildMetadataResponse())))
      status(result) shouldBe FORBIDDEN
    }

    "return a 404 when the user is logged in but the requested document doesn't exist" in {
      val controller = setupController(Some(validAuthority))
      mockAuthResourceNotFound(mockMetadataRepo, validAuthority)

      val result = call(controller.updateMetaData(regId), FakeRequest().withJsonBody(Json.toJson(buildMetadataResponse())))
      status(result) shouldBe NOT_FOUND
    }
  }


   "update last signed in" should {

    val regId = "0123456789"
    val currentTime = DateTime.now(DateTimeZone.UTC)

    val currentTimeCaptor = ArgumentCaptor.forClass[DateTime, DateTime](classOf[DateTime])

    "return a 200 if Json body can be parsed and last timestamp has been updated" in {
      val controller = setupController(Some(validAuthority))
      mockSuccessfulAuthorisation(mockMetadataRepo, regId, validAuthority)

      when(mockMetadataService.updateLastSignedIn(eqTo(regId), currentTimeCaptor.capture()))
        .thenReturn(Future.successful(currentTime))

      val result = call(controller.updateLastSignedIn(regId), FakeRequest().withJsonBody(Json.toJson(currentTime)))

      status(result) shouldBe OK

      Json.toJson[DateTime](currentTimeCaptor.getValue) shouldBe await(jsonBodyOf(result))
    }

    "return a 403 when the user is not logged in" in {
      val controller = setupController(None)
      mockNotLoggedIn(mockMetadataRepo)

      val result = call(controller.updateLastSignedIn(regId), FakeRequest().withJsonBody(Json.toJson(currentTime)))
      status(result) shouldBe FORBIDDEN
    }

    "return a 403 when the user is logged in but not authorised to access the resource" in {
      val controller = setupController(Some(validAuthority))
      mockNotAuthorised(mockMetadataRepo, regId, validAuthority)

      val result = call(controller.updateLastSignedIn(regId), FakeRequest().withJsonBody(Json.toJson(currentTime)))
      status(result) shouldBe FORBIDDEN
    }

    "return a 404 when the user is logged in but the requested document doesn't exist" in {
      val controller = setupController(Some(validAuthority))
      mockAuthResourceNotFound(mockMetadataRepo, validAuthority)

      val result = call(controller.updateLastSignedIn(regId), FakeRequest().withJsonBody(Json.toJson(currentTime)))
      status(result) shouldBe NOT_FOUND
    }
  }

  "remove Metadata rejected" should {

    val regId = "0123456789"

    "return a 200 if the regId is found and deleted" in {
      val controller = setupController(Some(validAuthority))
      val regIdCaptor = ArgumentCaptor.forClass[String, String](classOf[String])

      when(mockMetadataService.removeMetadata(regIdCaptor.capture()))
        .thenReturn(Future.successful(true))

      val result = call(controller.removeMetadata(regId), FakeRequest())

      status(result) shouldBe OK
      regIdCaptor.getValue shouldBe regId
    }
  }
}
