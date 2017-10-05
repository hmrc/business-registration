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

import com.codahale.metrics.{Counter, Timer}
import connectors.AuthConnector
import fixtures.{AuthFixture, MetadataFixture}
import helpers.{AuthMocks, SCRSControllerSpec}
import models.ErrorResponse
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.{MetadataMongo, MetadataRepositoryMongo}
import services.{MetadataService, MetricsService}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.ArgumentCaptor
import play.api.mvc.{Request, Result}

import scala.concurrent.Future
import scala.language.implicitConversions

class MetadataControllerSpec extends SCRSControllerSpec with BeforeAndAfterEach with AuthMocks
  with MetadataFixture with AuthFixture {

  val mockMetadataService: MetadataService = mock[MetadataService]
  val mockMetadataRepo: MetadataRepositoryMongo = mock[MetadataRepositoryMongo]
  val mockMetaDataMongo: MetadataMongo = mock[MetadataMongo]
  val mockMetricsService: MetricsService = mock[MetricsService]
  implicit val mockAuthConnector: AuthConnector = mock[AuthConnector]

  override protected def beforeEach(): Unit = {
    reset(
      mockAuthConnector,
      mockMetadataRepo,
      mockMetadataService,
      mockMetricsService
    )
  }

  trait Setup {
    val controller: MetadataController = new MetadataController {
      val metadataService: MetadataService = mockMetadataService
      val resourceConn: MetadataRepositoryMongo = mockMetadataRepo
      val metricsService: MetricsService = mockMetricsService
      val authConnector: AuthConnector = mockAuthConnector
    }
  }

  val regId = "0123456789"
  val timer = new Timer
  val counter = new Counter

  "calling createMetadata" should {

    "return a 201" in new Setup {

      when(mockMetadataService.createMetadataRecord(any(), any()))
        .thenReturn(Future.successful(buildMetadataResponse()))

      mockGetCurrentAuthority(Some(validAuthority))

      when(mockMetricsService.createFootprintCounter).thenReturn(counter)
      when(mockMetricsService.createMetadataTimer).thenReturn(timer)

      val request: Request[JsValue] = FakeRequest().withJsonBody(Json.toJson(buildMetadataRequest()))
      val result = controller.createMetadata(request)

      status(result) shouldBe CREATED

      contentType(result) shouldBe Some("application/json")
      bodyAsJson(result).as[JsObject] shouldBe metadataResponseJsObj
    }

    "return a 403 when the user is not authenticated" in new Setup {

      mockGetCurrentAuthority(None)

      when(mockMetricsService.createFootprintCounter).thenReturn(counter)

      val request: Request[JsValue] = FakeRequest().withJsonBody(validMetadataJson)
      val result = controller.createMetadata(request)

      status(result) shouldBe FORBIDDEN
    }
  }

  "searchMetadata" should {

    "return a 200 and a MetadataResponse as json if metadata is found" in new Setup {

      when(mockMetadataService.searchMetadataRecord(any()))
        .thenReturn(Future.successful(Some(buildMetadataResponse())))

      mockGetCurrentAuthority(Some(validAuthority))
      when(mockMetricsService.searchMetadataTimer).thenReturn(timer)

      val result = controller.searchMetadata(FakeRequest())
      status(result) shouldBe OK
      bodyAsJson(result).as[JsObject] shouldBe metadataResponseJsObj
    }

    "return a 403 - forbidden when the user is not authenticated" in new Setup {

      mockGetCurrentAuthority(None)

      val result: Result = controller.searchMetadata(FakeRequest())
      status(result) shouldBe FORBIDDEN
    }

    "return a 404 - NotFound when the resource doesn't exist" in new Setup {

      mockGetCurrentAuthority(Some(validAuthority))

      when(mockMetadataService.searchMetadataRecord(any()))
        .thenReturn(Future.successful(None))

      when(mockMetricsService.searchMetadataTimer).thenReturn(timer)

      val result = controller.searchMetadata(FakeRequest())
      status(result) shouldBe NOT_FOUND
      bodyAsJson(result) shouldBe ErrorResponse.MetadataNotFound
    }
  }

  "retrieveMetadata" should {

    "return a 200 and a metadata model is one is found" in new Setup {

      val regIdCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass[String, String](classOf[String])

      when(mockMetadataService.retrieveMetadataRecord(regIdCaptor.capture()))
        .thenReturn(Future.successful(Some(buildMetadataResponse())))

      when(mockMetricsService.retrieveMetadataTimer).thenReturn(timer)

      mockSuccessfulAuthorisation(mockMetadataRepo, regId, validAuthority)
      mockGetCurrentAuthority(Some(validAuthority))

      val result: Result = controller.retrieveMetadata(regId)(FakeRequest())

      status(result) shouldBe OK
      regIdCaptor.getValue shouldBe regId
      bodyAsJson(result) shouldBe metadataResponseJsObj
    }

    "return a 403 when the user is not logged in" in new Setup {

      val regIdCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass[String, String](classOf[String])

      when(mockMetadataService.retrieveMetadataRecord(regIdCaptor.capture()))
        .thenReturn(Future.successful(Some(buildMetadataResponse())))

      mockNotLoggedIn(mockMetadataRepo)
      mockGetCurrentAuthority(None)

      val result: Result = controller.retrieveMetadata(regId)(FakeRequest())
      status(result) shouldBe FORBIDDEN

      verify(mockMetadataService, times(0)).retrieveMetadataRecord(any())
    }

    "return a 403 when the user is logged in but not authorised to access the resource" in new Setup {

      mockNotAuthorised(mockMetadataRepo, regId, validAuthority)
      mockGetCurrentAuthority(Some(validAuthority))

      val result: Result = controller.retrieveMetadata(regId)(FakeRequest())
      status(result) shouldBe FORBIDDEN
    }

    "return a 404 when the user is logged in but the requested document doesn't exist" in new Setup {

      mockAuthResourceNotFound(mockMetadataRepo, validAuthority)
      mockGetCurrentAuthority(Some(validAuthority))

      val result: Result = controller.retrieveMetadata(regId)(FakeRequest())

      status(result) shouldBe NOT_FOUND
    }

    "return a 404 - not found logged in the requested document doesn't exist but got through auth" in new Setup {

      mockGetCurrentAuthority(Some(validAuthority))

      when(mockMetadataRepo.getInternalId(eqTo(regId))).
        thenReturn(Future.successful(Some((regId,validAuthority.ids.internalId))))

      when(mockMetadataService.retrieveMetadataRecord(eqTo(regId)))
        .thenReturn(Future.successful(None))

      when(mockMetricsService.retrieveMetadataTimer).thenReturn(timer)

      val result: Result = controller.retrieveMetadata(regId)(FakeRequest())

      status(result) shouldBe NOT_FOUND
      bodyAsJson(result) shouldBe ErrorResponse.MetadataNotFound
    }
  }

  "updateMetaData" should {

    "return a 200 if Json body can be parsed and meta data has been updated" in new Setup {

      mockGetCurrentAuthority(Some(validAuthority))
      mockSuccessfulAuthorisation(mockMetadataRepo, regId, validAuthority)

      when(mockMetadataService.updateMetaDataRecord(eqTo(regId), eqTo(buildMetadataResponse())))
        .thenReturn(Future.successful(buildMetadataResponse()))

      when(mockMetricsService.updateMetadataTimer).thenReturn(timer)

      val request: Request[JsValue] = FakeRequest().withJsonBody(Json.toJson(buildMetadataResponse()))
      val result: Result = controller.updateMetaData(regId)(request)

      status(result) shouldBe OK
      bodyAsJson(result).as[JsObject] shouldBe metadataResponseJsObj
    }

    "return a 403 when the user is not logged in" in new Setup {

      mockGetCurrentAuthority(None)
      mockNotLoggedIn(mockMetadataRepo)

      val request: Request[JsValue] = FakeRequest().withJsonBody(Json.toJson(buildMetadataResponse()))
      val result: Result = controller.updateMetaData(regId)(request)

      status(result) shouldBe FORBIDDEN
    }

    "return a 403 when the user is logged in but not authorised to access the resource" in new Setup {

      mockGetCurrentAuthority(Some(validAuthority))
      mockNotAuthorised(mockMetadataRepo, regId, validAuthority)

      val request: Request[JsValue] = FakeRequest().withJsonBody(Json.toJson(buildMetadataResponse()))
      val result: Result = controller.updateMetaData(regId)(request)

      status(result) shouldBe FORBIDDEN
    }

    "return a 404 when the user is logged in but the requested document doesn't exist" in new Setup {

      mockGetCurrentAuthority(Some(validAuthority))
      mockAuthResourceNotFound(mockMetadataRepo, validAuthority)

      val request: Request[JsValue] = FakeRequest().withJsonBody(Json.toJson(buildMetadataResponse()))
      val result: Result = controller.updateMetaData(regId)(request)

      status(result) shouldBe NOT_FOUND
    }
  }

  "update last signed in" should {

    val currentTime = DateTime.now(DateTimeZone.UTC)
    val currentTimeCaptor = ArgumentCaptor.forClass[DateTime, DateTime](classOf[DateTime])

    "return a 200 if Json body can be parsed and last timestamp has been updated" in new Setup {

      mockGetCurrentAuthority(Some(validAuthority))
      mockSuccessfulAuthorisation(mockMetadataRepo, regId, validAuthority)

      when(mockMetadataService.updateLastSignedIn(eqTo(regId), currentTimeCaptor.capture()))
        .thenReturn(Future.successful(currentTime))

      val request: Request[JsValue] = FakeRequest().withJsonBody(Json.toJson(currentTime))
      val result: Result = controller.updateLastSignedIn(regId)(request)

      status(result) shouldBe OK
      Json.toJson[DateTime](currentTimeCaptor.getValue) shouldBe bodyAsJson(result)
    }

    "return a 403 when the user is not logged in" in new Setup {

      mockGetCurrentAuthority(None)
      mockNotLoggedIn(mockMetadataRepo)

      val request: Request[JsValue] = FakeRequest().withJsonBody(Json.toJson(currentTime))
      val result: Result = controller.updateLastSignedIn(regId)(request)

      status(result) shouldBe FORBIDDEN
    }

    "return a 403 when the user is logged in but not authorised to access the resource" in new Setup {

      mockGetCurrentAuthority(Some(validAuthority))
      mockNotAuthorised(mockMetadataRepo, regId, validAuthority)

      val request: Request[JsValue] = FakeRequest().withJsonBody(Json.toJson(currentTime))
      val result: Result = controller.updateLastSignedIn(regId)(request)

      status(result) shouldBe FORBIDDEN
    }

    "return a 404 when the user is logged in but the requested document doesn't exist" in new Setup {

      mockGetCurrentAuthority(Some(validAuthority))
      mockAuthResourceNotFound(mockMetadataRepo, validAuthority)

      val request: Request[JsValue] = FakeRequest().withJsonBody(Json.toJson(currentTime))
      val result: Result = controller.updateLastSignedIn(regId)(request)

      status(result) shouldBe NOT_FOUND
    }
  }

  "remove Metadata rejected" should {

    "return a 200 if the regId is found and deleted" in new Setup {

      mockGetCurrentAuthority(Some(validAuthority))

      when(mockMetricsService.removeMetadataTimer).thenReturn(timer)

      val regIdCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass[String, String](classOf[String])

      when(mockMetadataService.removeMetadata(regIdCaptor.capture()))
        .thenReturn(Future.successful(true))

      val result: Result = controller.removeMetadata(regId)(FakeRequest())

      status(result) shouldBe OK
      regIdCaptor.getValue shouldBe regId
    }
  }
}
