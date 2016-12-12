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

package controllers

import connectors.AuthConnector
import fixtures.{AuthFixture, MetadataFixture}
import helpers.SCRSSpec
import mocks.MetricServiceMock
import models.{ErrorResponse, Metadata, MetadataResponse}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.FakeRequest
import services.{MetadataService, MetricsService}
import play.api.mvc.Results.{Created, Ok}
import play.api.test.Helpers._

import scala.concurrent.Future
import org.mockito.Mockito._
import org.mockito.Matchers

class MetadataControllerSpec extends SCRSSpec with MetadataFixture with AuthFixture {

  class Setup {
    val controller = new MetadataController {
      val metadataService = mockMetadataService
      val resourceConn = mockMetadataRepository
      val auth = mockAuthConnector
      override val metricsService: MetricsService = MetricServiceMock
    }
  }

  "MetadataController" should {
    "use the correct MetadataService" in {
      MetadataController.metadataService shouldBe MetadataService
    }
    "use the correct auth connector" in {
      MetadataController.auth shouldBe AuthConnector
    }
  }

  "createMetadata" should {

    "return a 201 when a new entry is created from the parsed json" in new Setup {
      MetadataServiceMocks.createMetadataRecord(buildMetadataResponse())
      AuthenticationMocks.getCurrentAuthority(Some(validAuthority))

      val request = FakeRequest().withJsonBody(Json.toJson(buildMetadataRequest()))
      val result = call(controller.createMetadata, request)
      await(jsonBodyOf(result)).as[JsObject] shouldBe metadataResponseJsObj
      status(result) shouldBe CREATED
    }

    "return a 403 - forbidden when the user is not authenticated" in new Setup {
      AuthenticationMocks.getCurrentAuthority(None)

      val request = FakeRequest().withJsonBody(validMetadataJson)
      val result = call(controller.createMetadata, request)
      status(result) shouldBe FORBIDDEN
    }
  }

  "searchMetadata" should {

    "return a 200 and a MetadataResponse as json if metadata is found" in new Setup {
      AuthenticationMocks.getCurrentAuthority(Some(validAuthority))
      MetadataServiceMocks.searchMetadataRecord(validAuthority.ids.internalId, Some(buildMetadataResponse()))

      val result = call(controller.searchMetadata, FakeRequest())
      status(result) shouldBe OK
      await(jsonBodyOf(result)).as[JsObject] shouldBe metadataResponseJsObj
    }

    "return a 403 - forbidden when the user is not authenticated" in new Setup {
      AuthenticationMocks.getCurrentAuthority(None)

      val result = call(controller.searchMetadata, FakeRequest())
      status(result) shouldBe FORBIDDEN
    }

    "return a 404 - NotFound when the resource doesn't exist" in new Setup {
      AuthenticationMocks.getCurrentAuthority(Some(validAuthority))
      MetadataServiceMocks.searchMetadataRecord(validAuthority.ids.internalId, None)

      val result = call(controller.searchMetadata, FakeRequest())
      status(result) shouldBe NOT_FOUND
      await(jsonBodyOf(result)) shouldBe ErrorResponse.MetadataNotFound
    }
  }

  "retrieveMetadata" should {

    val regId = "0123456789"

    "return a 200 and a metadata model is one is found" in new Setup {
      AuthorisationMock.mockSuccessfulAuthorisation(regId, validAuthority)
      MetadataServiceMocks.retrieveMetadataRecord(regId, Some(buildMetadataResponse()))

      val result = call(controller.retrieveMetadata(regId), FakeRequest())
      status(result) shouldBe OK
      await(jsonBodyOf(result)).as[JsValue] shouldBe metadataResponseJsObj
    }

    "return a 403 when the user is not logged in" in new Setup {
      AuthorisationMock.mockNotLoggedIn

      val result = call(controller.retrieveMetadata(regId), FakeRequest())
      status(result) shouldBe FORBIDDEN
    }

    "return a 403 when the user is logged in but not authorised to access the resource" in new Setup {
      AuthorisationMock.mockNotAuthorised(regId, validAuthority)

      val result = call(controller.retrieveMetadata(regId), FakeRequest())
      status(result) shouldBe FORBIDDEN
    }

    "return a 404 when the user is logged in but the requested document doesn't exist" in new Setup {
      AuthorisationMock.mockAuthResourceNotFound(validAuthority)

      val result = call(controller.retrieveMetadata(regId), FakeRequest())
      status(result) shouldBe NOT_FOUND
    }

    "return a 404 - not found logged in the requested document doesn't exist but got through auth" in new Setup {
      AuthenticationMocks.getCurrentAuthority(Some(validAuthority))
      when(mockMetadataRepository.getInternalId(Matchers.eq(regId))).
        thenReturn(Future.successful(Some((regId,validAuthority.ids.internalId))))
      MetadataServiceMocks.retrieveMetadataRecord(regId, None)

      val result = call(controller.retrieveMetadata(regId), FakeRequest())
      status(result) shouldBe NOT_FOUND
      await(jsonBodyOf(result)) shouldBe ErrorResponse.MetadataNotFound
    }
  }

  "updateMetaData" should {

    val regId = "0123456789"

    "return a 200 if Json body can be parsed and meta data has been updated" in new Setup {
      AuthorisationMock.mockSuccessfulAuthorisation(regId, validAuthority)

      when(mockMetadataService.updateMetaDataRecord(Matchers.eq(regId), Matchers.eq(buildMetadataResponse())))
        .thenReturn(Future.successful(buildMetadataResponse()))

      val result = call(controller.updateMetaData(regId), FakeRequest().withJsonBody(Json.toJson(buildMetadataResponse())))
      status(result) shouldBe OK
      await(jsonBodyOf(result)).as[JsObject] shouldBe metadataResponseJsObj
    }

    "return a 403 when the user is not logged in" in new Setup {
      AuthorisationMock.mockNotLoggedIn

      val result = call(controller.updateMetaData(regId), FakeRequest().withJsonBody(Json.toJson(buildMetadataResponse())))
      status(result) shouldBe FORBIDDEN
    }

    "return a 403 when the user is logged in but not authorised to access the resource" in new Setup {
      AuthorisationMock.mockNotAuthorised(regId, validAuthority)

      val result = call(controller.updateMetaData(regId), FakeRequest().withJsonBody(Json.toJson(buildMetadataResponse())))
      status(result) shouldBe FORBIDDEN
    }

    "return a 404 when the user is logged in but the requested document doesn't exist" in new Setup {
      AuthorisationMock.mockAuthResourceNotFound(validAuthority)

      val result = call(controller.updateMetaData(regId), FakeRequest().withJsonBody(Json.toJson(buildMetadataResponse())))
      status(result) shouldBe NOT_FOUND
    }
  }
}
