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

package controllers

import auth.AuthorisationResource
import fixtures.{AuthFixture, MetadataFixture}
import helpers.{AuthMocks, SCRSSpec}
import mocks.MetricServiceMock
import models.ErrorResponse
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.{MetadataMongo, MetadataRepository, MetadataRepositoryMongo}
import services.{MetadataService, MetricsService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class MetadataControllerSpec extends SCRSSpec with MetadataFixture with AuthFixture with AuthMocks with MockitoSugar {

  val mockMetadataService = mock[MetadataService]
  val mockMetadataRepo = mock[MetadataRepositoryMongo]
  val mockMetadataAuthRepo = mock[MetadataRepository]
  val mockMetaDataMongo = mock[MetadataMongo]

  class Setup{
    val controller = new MetadataController {
      val metadataService: MetadataService = mockMetadataService
      val metricsService: MetricsService = MetricServiceMock
      val resourceConn: AuthorisationResource = mockMetadataRepo

      val authConnector = mockAuthConnector
    }
  }

  implicit val hc = HeaderCarrier()

  override protected def beforeEach() = {
    reset(
      mockAuthConnector,
      mockMetadataRepo,
      mockMetadataService
    )
  }


  "calling createMetadata" should {

    "return a 201" in new Setup {

      mockSuccessfulAuthentication

      when(mockMetadataService.createMetadataRecord(any(), any())(any()))
        .thenReturn(Future.successful(buildMetadataResponse()))

      val metadataRequest = buildMetadataRequest()
      val request = FakeRequest().withBody[JsValue](Json.toJson(metadataRequest))
      val result = controller.createMetadata(request)

      status(result) mustBe CREATED

      contentType(result) mustBe Some("application/json")
      bodyAsJson(result) mustBe metadataResponseJsObj
    }

    "return a 403 when the user is not authenticated" in new Setup {
      mockNotLoggedIn

      val request = FakeRequest().withBody[JsValue](validMetadataJson)
      val result = controller.createMetadata(request)

      status(result) mustBe FORBIDDEN
    }
  }

  "searchMetadata" should {

    "return a 200 and a MetadataResponse as json if metadata is found" in new Setup {

      mockSuccessfulAuthentication

      when(mockMetadataService.searchMetadataRecord(any())(any()))
        .thenReturn(Future.successful(Some(buildMetadataResponse())))

      val result = controller.searchMetadata(FakeRequest())

      status(result) mustBe OK
      bodyAsJson(result) mustBe metadataResponseJsObj
    }

    "return a 403 - forbidden when the user is not authenticated" in new Setup {
      mockNotLoggedIn

      val result = controller.searchMetadata(FakeRequest())
      status(result) mustBe FORBIDDEN
    }

    "return a 404 - NotFound when the resource doesn't exist" in new Setup {
      mockSuccessfulAuthentication

      when(mockMetadataService.searchMetadataRecord(any())(any()))
        .thenReturn(Future.successful(None))

      val result = controller.searchMetadata(FakeRequest())
      status(result) mustBe NOT_FOUND
      bodyAsJson(result) mustBe ErrorResponse.MetadataNotFound
    }
  }

  "retrieveMetadata" should {

    val regId = "0123456789"

    "return a 200 and a metadata model is one is found" in new Setup {

      val regIdCaptor = ArgumentCaptor.forClass[String, String](classOf[String])

      when(mockMetadataService.retrieveMetadataRecord(regIdCaptor.capture())(any()))
        .thenReturn(Future.successful(Some(buildMetadataResponse())))

      mockSuccessfulAuthorisation(mockMetadataRepo, regId)

      val result = controller.retrieveMetadata(regId)(FakeRequest())

      status(result) mustBe OK
      regIdCaptor.getValue mustBe regId
      bodyAsJson(result) mustBe metadataResponseJsObj
    }

    "return a 403 when the user is not logged in" in new Setup {

      val regIdCaptor = ArgumentCaptor.forClass[String, String](classOf[String])

      when(mockMetadataService.retrieveMetadataRecord(regIdCaptor.capture())(any()))
        .thenReturn(Future.successful(Some(buildMetadataResponse())))

      mockNotLoggedIn

      val result = controller.retrieveMetadata("noitisnot")(FakeRequest())
      status(result) mustBe FORBIDDEN

      verify(mockMetadataService, times(0)).retrieveMetadataRecord(any())(any())
    }

    "return a 403 when the user is logged in but not authorised to access the resource" in new Setup {
      mockNotAuthorised(mockMetadataRepo, regId)

      val result = controller.retrieveMetadata(regId)(FakeRequest())
      status(result) mustBe FORBIDDEN
    }

    "return a 404 when the user is logged in but the requested document doesn't exist" in new Setup {
      mockAuthResourceNotFound(mockMetadataRepo)

      val result = controller.retrieveMetadata(regId)(FakeRequest())
      status(result) mustBe NOT_FOUND
    }

    "return a 404 - not found logged in the requested document doesn't exist but got through auth" in new Setup {
      mockSuccessfulAuthentication

      when(mockMetadataRepo.getInternalId(eqTo(regId))(any())).
        thenReturn(Future.successful(Some("internal-id")))

      when(mockMetadataService.retrieveMetadataRecord(eqTo(regId))(any()))
        .thenReturn(Future.successful(None))

      val result = controller.retrieveMetadata(regId)(FakeRequest())
      status(result) mustBe NOT_FOUND
      bodyAsJson(result) mustBe ErrorResponse.MetadataNotFound
    }
  }

  "updateMetaData" should {

    val regId = "0123456789"

    "return a 200 if Json body can be parsed and meta data has been updated" in new Setup {
      mockSuccessfulAuthorisation(mockMetadataRepo, regId)


      when(mockMetadataService.updateMetaDataRecord(eqTo(regId), eqTo(buildMetadataResponse()))(any()))
        .thenReturn(Future.successful(buildMetadataResponse()))

      val result = controller.updateMetaData(regId)(FakeRequest().withBody[JsValue](Json.toJson(buildMetadataResponse())))
      status(result) mustBe OK
      bodyAsJson(result) mustBe metadataResponseJsObj
    }

    "return a 403 when the user is not logged in" in new Setup {
      mockNotLoggedIn


      val result = controller.updateMetaData(regId)(FakeRequest().withBody[JsValue](Json.toJson(buildMetadataResponse())))
      status(result) mustBe FORBIDDEN
    }

    "return a 403 when the user is logged in but not authorised to access the resource" in new Setup {
      mockNotAuthorised(mockMetadataRepo, regId)


      val result = controller.updateMetaData(regId)(FakeRequest().withBody[JsValue](Json.toJson(buildMetadataResponse())))
      status(result) mustBe FORBIDDEN
    }

    "return a 404 when the user is logged in but the requested document doesn't exist" in new Setup {
      mockAuthResourceNotFound(mockMetadataRepo)


      val result = controller.updateMetaData(regId)(FakeRequest().withBody[JsValue](Json.toJson(buildMetadataResponse())))
      status(result) mustBe NOT_FOUND
    }
  }


   "update last signed in" should {

    val regId = "0123456789"
    val currentTime = DateTime.now(DateTimeZone.UTC)

    val currentTimeCaptor = ArgumentCaptor.forClass[DateTime, DateTime](classOf[DateTime])

    "return a 200 if Json body can be parsed and last timestamp has been updated" in new Setup {
      mockSuccessfulAuthorisation(mockMetadataRepo, regId)


      when(mockMetadataService.updateLastSignedIn(eqTo(regId), currentTimeCaptor.capture())(any()))
        .thenReturn(Future.successful(currentTime))

      val result = controller.updateLastSignedIn(regId)(FakeRequest().withBody[JsValue](Json.toJson(currentTime)))

      status(result) mustBe OK

      Json.toJson[DateTime](currentTimeCaptor.getValue) mustBe bodyAsJson(result)
    }

    "return a 403 when the user is not logged in" in new Setup {
      mockNotLoggedIn

      val result = controller.updateLastSignedIn(regId)(FakeRequest().withBody[JsValue](Json.toJson(currentTime)))
      status(result) mustBe FORBIDDEN
    }

    "return a 403 when the user is logged in but not authorised to access the resource" in new Setup {
      mockNotAuthorised(mockMetadataRepo, regId)


      val result = controller.updateLastSignedIn(regId)(FakeRequest().withBody[JsValue](Json.toJson(currentTime)))
      status(result) mustBe FORBIDDEN
    }

    "return a 404 when the user is logged in but the requested document doesn't exist" in new Setup {
      mockAuthResourceNotFound(mockMetadataRepo)


      val result = controller.updateLastSignedIn(regId)(FakeRequest().withBody[JsValue](Json.toJson(currentTime)))
      status(result) mustBe NOT_FOUND
    }
  }

  "remove Metadata rejected" should {

    val regId = "0123456789"

    "return a 200 if the regId is found and deleted" in new Setup {
      mockSuccessfulAuthorisation(mockMetadataRepo, regId)
      val regIdCaptor = ArgumentCaptor.forClass[String, String](classOf[String])

      when(mockMetadataService.removeMetadata(regIdCaptor.capture())(any()))
        .thenReturn(Future.successful(true))

      val result = controller.removeMetadata(regId)(FakeRequest())

      status(result) mustBe OK
      regIdCaptor.getValue mustBe regId
    }

    "return a Forbidden if the logged in user is not authorised" in new Setup {
      mockNotAuthorised(mockMetadataRepo, regId)

      val result = controller.removeMetadata(regId)(FakeRequest())

      status(result) mustBe FORBIDDEN
    }

    "return a Forbidden if the user is not logged in" in new Setup {
      mockNotLoggedIn

      val result = controller.removeMetadata(regId)(FakeRequest())

      status(result) mustBe FORBIDDEN
    }

    "return a NotFound if the logged in user is not authorised" in new Setup {
      mockAuthResourceNotFound(mockMetadataRepo)

      val result = controller.removeMetadata(regId)(FakeRequest())

      status(result) mustBe NOT_FOUND
    }
  }
}
