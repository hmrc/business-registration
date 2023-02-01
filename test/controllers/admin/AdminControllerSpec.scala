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

package controllers.admin

import fixtures.MetadataFixture
import helpers.SCRSSpec
import models.ErrorResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import services.MetadataService

import scala.concurrent.Future

class AdminControllerSpec extends SCRSSpec with MetadataFixture {

  val mockMetadataService: MetadataService = mock[MetadataService]

  class Setup {
    val controller = new AdminController(mockMetadataService, stubControllerComponents())

    when(mockMetadataService.buildSelfLink(any()))
      .thenReturn(buildSelfLinkJsObj())
  }

  "retrieve business registration" should {
    "return an Ok with a metadata response" in new Setup {
      when(mockMetadataService.retrieveMetadataRecord(any())(any()))
        .thenReturn(Future.successful(Some(buildMetadataResponse())))

      val result: Future[Result] = controller.retrieveBusinessRegistration("12345")(FakeRequest())

      status(result) mustBe OK
      bodyAsJson(result) mustBe metadataResponseJsObj
    }

    "return and NotFound with an error code if no metadata is found" in new Setup {
      when(mockMetadataService.retrieveMetadataRecord(any())(any()))
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.retrieveBusinessRegistration("12345")(FakeRequest())

      status(result) mustBe NOT_FOUND
      bodyAsJson(result) mustBe Json.toJson(ErrorResponse("404", "Could not find metadata record"))
    }

    "throw an exception if an exception occurs in retrieving the metadata" in new Setup {
      when(mockMetadataService.retrieveMetadataRecord(any())(any()))
        .thenReturn(Future.failed(new IllegalStateException()))

      val result: Future[Result] = controller.retrieveBusinessRegistration("12345")(FakeRequest())

      intercept[IllegalStateException](await(result))
    }
  }

  "remove metadata" should {
    "return an Ok" when {
      "when metadata was successfully deleted" in new Setup {
        when(mockMetadataService.removeMetadata(any())(any()))
          .thenReturn(Future.successful(true))

        status(controller.removeMetadata("12345")(FakeRequest())) mustBe OK
      }
    }

    "return NotFound" when {
      "no metadata is found" in new Setup {
        when(mockMetadataService.removeMetadata(any())(any()))
          .thenReturn(Future.successful(false))

        status(controller.removeMetadata("12345")(FakeRequest())) mustBe NOT_FOUND
      }

      "throw an exception" when {
        "an exception occurs in retrieving the metadata" in new Setup {
          when(mockMetadataService.removeMetadata(any())(any()))
            .thenReturn(Future.failed(new IllegalStateException()))

          intercept[IllegalStateException](await(controller.removeMetadata("12345")(FakeRequest())))
        }
      }
    }
  }
}