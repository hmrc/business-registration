/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.prePop

import auth.AuthorisationResource
import helpers.{AuthMocks, SCRSSpec}
import models.prepop.PermissionDenied
import org.mockito.ArgumentMatchers
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import repositories.prepop.TradingNameRepository
import uk.gov.hmrc.auth.core.AuthConnector
import org.mockito.Mockito._

import scala.concurrent.Future

class TradingNameControllerSpec extends SCRSSpec with AuthMocks {

  val mockTradingNameRepository = mock[TradingNameRepository]

  class Setup {
    val controller = new TradingNameController {
      override def authConnector: AuthConnector = mockAuthConnector
      override val tradingNameRepo: TradingNameRepository = mockTradingNameRepository
    }
  }

  "calling getTradingName" should {
    "return a 204 if no trading name is found" in new Setup {
      mockSuccessfulAuthentication

      when(mockTradingNameRepository.getTradingName(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      val result = controller.getTradingName("regId")(FakeRequest())
      status(result) mustBe NO_CONTENT
    }

    "return a trading name if one is found" in new Setup {
      mockSuccessfulAuthentication

      when(mockTradingNameRepository.getTradingName(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some("foo bar wizz")))

      val result = controller.getTradingName("regId")(FakeRequest())
      status(result) mustBe OK
      bodyAsJson(result) mustBe Json.obj("tradingName" -> "foo bar wizz")
    }

    "return a 403 if the user is not authorised" in new Setup {
      mockSuccessfulAuthentication

      when(mockTradingNameRepository.getTradingName(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.failed(PermissionDenied("regId", "intId")))

      val result = controller.getTradingName("regId")(FakeRequest())
      status(result) mustBe FORBIDDEN
    }

    "return a 403 if the user is not logged in" in new Setup {
      mockNotLoggedIn
      val result = controller.getTradingName("regId")(FakeRequest())
      status(result) mustBe FORBIDDEN
    }
  }

  "calling upsertTradingName" should {
    "return the trading name that was passed in" in new Setup {
      mockSuccessfulAuthentication

      when(mockTradingNameRepository.upsertTradingName(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some("new foo bar wizz")))

      val result = controller.upsertTradingName("regId")(FakeRequest().withBody(Json.obj("tradingName" -> "new foo bar wizz")))
      status(result) mustBe OK
      bodyAsJson(result) mustBe Json.obj("tradingName" -> "new foo bar wizz")
    }

    "return a 400 for incorrect json passed in" in new Setup {
      mockSuccessfulAuthentication

      when(mockTradingNameRepository.upsertTradingName(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      val result = controller.upsertTradingName("regId")(FakeRequest().withBody(Json.obj("tracingName" -> "new foo bar wizz")))
      status(result) mustBe BAD_REQUEST
    }

    "return a 500 if no data is returned from the repo" in new Setup {
      mockSuccessfulAuthentication

      when(mockTradingNameRepository.upsertTradingName(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      val result = controller.upsertTradingName("regId")(FakeRequest().withBody(Json.obj("tradingName" -> "new foo bar wizz")))
      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return a 403 if the user is not authorised" in new Setup {
      mockSuccessfulAuthentication

      when(mockTradingNameRepository.upsertTradingName(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.failed(PermissionDenied("regId", "intId")))

      val result = controller.upsertTradingName("regId")(FakeRequest().withBody(Json.obj("tradingName" -> "new foo bar wizz")))
      status(result) mustBe FORBIDDEN
    }

    "return a 403 if the user is not logged in" in new Setup {
      mockNotLoggedIn
      val result = controller.upsertTradingName("regId")(FakeRequest().withBody(Json.obj("tradingName" -> "new foo bar wizz")))
      status(result) mustBe FORBIDDEN
    }
  }
}
