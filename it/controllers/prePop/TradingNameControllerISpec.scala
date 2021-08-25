/*
 * Copyright 2021 HM Revenue & Customs
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

import fixtures.MetadataFixtures
import itutil.IntegrationSpecBase
import models.prepop.TradingName
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.play.json.ImplicitBSONHandlers._
import repositories.prepop.TradingNameRepository
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TradingNameControllerISpec extends IntegrationSpecBase with MetadataFixtures {

  class Setup {
    lazy val tradingNameRepository: TradingNameRepository = app.injector.instanceOf[TradingNameRepository]
    lazy val authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

    val controller: TradingNameController = new TradingNameController(tradingNameRepository, authConnector, stubControllerComponents())

    def dropTradingName(intId: String = testInternalId, regId: String = testRegistrationId): WriteResult =
      await(tradingNameRepository.collection.remove(Json.obj("_id" -> regId)))

    def insertTradingName(regId: String = testRegistrationId, intId: String = testInternalId, tradingName: String = "foo bar wizz"): UpdateWriteResult =
      await(tradingNameRepository.collection.update(
        Json.obj("_id" -> regId, "internalId" -> intId), TradingName.mongoWrites(regId, intId).writes(tradingName), upsert = true)
      )

    def getTradingName(regId: String = testRegistrationId, intId: String = testInternalId): String =
      await(tradingNameRepository.collection.find(Json.obj("_id" -> regId, "internalId" -> intId)).one[JsObject].map {
        case Some(x) => x.as[String](TradingName.mongoTradingNameReads)
      })

    dropTradingName()
  }

  val invalidTradingNameJson: JsObject = Json.obj("tracingName" -> "foo bar wizz")
  val validTradingNameJson: JsObject = Json.obj("tradingName" -> "foo bar wizz")

  "calling getTradingName" should {
    "return a 204 if no trading name is found" in new Setup {
      stubSuccessfulLogin

      val result: Future[Result] = controller.getTradingName(testRegistrationId)(FakeRequest())

      status(result) mustBe NO_CONTENT
    }

    "return a trading name if one is found" in new Setup {
      stubSuccessfulLogin
      insertTradingName()

      val result: Future[Result] = controller.getTradingName(testRegistrationId)(FakeRequest())

      status(result) mustBe OK
      bodyAsJson(result) mustBe Json.obj("tradingName" -> "foo bar wizz")
    }

    "return a 403 if the user is not authorised" in new Setup {
      stubSuccessfulLogin
      insertTradingName(intId = "SomethingWrong")

      val result: Future[Result] = controller.getTradingName(testRegistrationId)(FakeRequest())

      status(result) mustBe FORBIDDEN
    }

    "return a 403 if the user is not logged in" in new Setup {
      stubNotLoggedIn

      val result: Future[Result] = controller.getTradingName(testRegistrationId)(FakeRequest())

      status(result) mustBe FORBIDDEN
    }
  }

  "calling upsertTradingName" should {
    "return the trading name that was passed in" in new Setup {
      stubSuccessfulLogin

      val result: Future[Result] = controller.upsertTradingName(testRegistrationId)(FakeRequest().withBody(Json.obj("tradingName" -> "new foo bar wizz")))

      status(result) mustBe OK
      bodyAsJson(result) mustBe Json.obj("tradingName" -> "new foo bar wizz")
      getTradingName() mustBe "new foo bar wizz"
    }

    "return a 400 for incorrect json passed in" in new Setup {
      stubSuccessfulLogin

      val result: Future[Result] = controller.upsertTradingName(testRegistrationId)(FakeRequest().withBody(Json.obj("tracingName" -> "new foo bar wizz")))

      status(result) mustBe BAD_REQUEST
    }

    "return a 403 if the user is not authorised" in new Setup {
      stubSuccessfulLogin
      insertTradingName(intId = "oooooops")

      val result: Future[Result] = controller.upsertTradingName(testRegistrationId)(FakeRequest().withBody(Json.obj("tradingName" -> "new foo bar wizz")))

      status(result) mustBe FORBIDDEN
    }

    "return a 403 if the user is not logged in" in new Setup {
      stubNotLoggedIn

      val result: Future[Result] = controller.upsertTradingName(testRegistrationId)(FakeRequest().withBody(Json.obj("tradingName" -> "new foo bar wizz")))

      status(result) mustBe FORBIDDEN
    }
  }
}
