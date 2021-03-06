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

import fixtures.MetadataFixtures
import itutil.IntegrationSpecBase
import models.Metadata
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.ImplicitBSONHandlers._
import repositories.MetadataMongoRepository
import services.{MetadataService, MetricsService}
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MetadataControllerISpec extends IntegrationSpecBase with MetadataFixtures {

  class Setup {
    lazy val metadataService: MetadataService = app.injector.instanceOf[MetadataService]
    lazy val metricsService: MetricsService = app.injector.instanceOf[MetricsService]
    lazy val metadataMongoRepository: MetadataMongoRepository = app.injector.instanceOf[MetadataMongoRepository]
    lazy val authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

    val controller = new MetadataController(metadataService, metricsService, metadataMongoRepository, authConnector, stubControllerComponents())

    def dropMetadata(internalId: String = testInternalId): WriteResult =
      await(metadataMongoRepository.collection.remove(Json.obj("internalId" -> internalId)))

    def insertMetadata(metadata: Metadata = buildMetadata()): Future[Metadata] =
      metadataMongoRepository.createMetadata(metadata)

    def getMetadata(regId: String = testRegistrationId): Future[Option[Metadata]] =
      metadataMongoRepository.retrieveMetadata(regId)

    dropMetadata()
  }

  "calling createMetadata" should {
    "return a 201" in new Setup {
      stubSuccessfulLogin

      val result: Future[Result] = controller.createMetadata()(FakeRequest().withBody[JsValue](buildMetadataJson()))

      status(result) mustBe CREATED
    }

    "return a 400 with an invalid Json" in new Setup {
      stubSuccessfulLogin

      val result: Future[Result] = controller.createMetadata()(FakeRequest().withBody[JsValue](Json.parse("""{}""")))

      status(result) mustBe BAD_REQUEST
    }

    "return a Forbidden if the user is not LoggedIn" in new Setup {
      stubNotLoggedIn

      val result: Future[Result] = controller.createMetadata()(FakeRequest().withBody[JsValue](buildMetadataJson("unmatchedInternalId")))

      status(result) mustBe FORBIDDEN
      dropMetadata("unmatchedInternalId")
    }
  }

  "calling searchMetadata" should {
    "return a NotFound if no data is found" in new Setup {
      stubSuccessfulLogin

      val result: Future[Result] = controller.searchMetadata(FakeRequest())
      status(result) mustBe NOT_FOUND
    }

    "return a 200 with a response body" in new Setup {
      stubSuccessfulLogin
      await(insertMetadata())

      val result: Future[Result] = controller.searchMetadata()(FakeRequest())

      status(result) mustBe OK
    }

    "return a Forbidden if the user is not LoggedIn" in new Setup {
      stubNotLoggedIn

      val result: Future[Result] = controller.searchMetadata()(FakeRequest())

      status(result) mustBe FORBIDDEN
    }
  }

  "calling retrieveMetadata" should {
    "return a NotFound if no data is found" in new Setup {
      stubSuccessfulLogin

      val result: Future[Result] = controller.retrieveMetadata(testRegistrationId)(FakeRequest())

      status(result) mustBe NOT_FOUND
    }

    "return a 200 with a response body" in new Setup {
      stubSuccessfulLogin
      await(insertMetadata())

      val result: Future[Result] = controller.retrieveMetadata(testRegistrationId)(FakeRequest())

      status(result) mustBe OK
    }

    "return a Forbidden if the user is not LoggedIn" in new Setup {
      stubNotLoggedIn

      val result: Future[Result] = controller.retrieveMetadata(testRegistrationId)(FakeRequest())

      status(result) mustBe FORBIDDEN
    }
  }

  "calling removeMetadata" should {
    "return a NotFound if no data is found" in new Setup {
      stubSuccessfulLogin

      val result: Future[Result] = controller.retrieveMetadata(testRegistrationId)(FakeRequest())

      status(result) mustBe NOT_FOUND
    }

    "return a 200 with a response body" in new Setup {
      stubSuccessfulLogin
      await(insertMetadata())

      val result: Future[Result] = controller.retrieveMetadata(testRegistrationId)(FakeRequest())

      status(result) mustBe OK
    }

    "return a Forbidden if the user is not LoggedIn" in new Setup {
      stubNotLoggedIn

      val result: Future[Result] = controller.retrieveMetadata(testRegistrationId)(FakeRequest())

      status(result) mustBe FORBIDDEN
    }
  }

  "calling updateMetadata" should {
    "return a NotFound if no data is found" in new Setup {
      stubSuccessfulLogin

      val result: Future[Result] = controller.updateMetaData(testRegistrationId)(FakeRequest().withBody[JsValue](buildMetadataResponseJson()))

      status(result) mustBe NOT_FOUND
    }

    "return a BadRequest if an invalid json is supplied" in new Setup {
      stubSuccessfulLogin
      await(insertMetadata())

      val result: Future[Result] = controller.updateMetaData(testRegistrationId)(FakeRequest().withBody[JsValue](invalidUpdateJson))

      status(result) mustBe BAD_REQUEST
    }

    "return a 200 with a response body" in new Setup {
      stubSuccessfulLogin
      await(insertMetadata())

      val result: Future[Result] = controller.updateMetaData(testRegistrationId)(FakeRequest().withBody[JsValue](buildMetadataResponseJson(cc = "Guardian")))

      status(result) mustBe OK
      val json: Option[Metadata] = await(getMetadata())
      json.isEmpty mustBe false
      json.get.completionCapacity mustBe Some("Guardian")
    }

    "return a Forbidden if the user is not LoggedIn" in new Setup {
      stubNotLoggedIn

      val result: Future[Result] = controller.updateMetaData(testRegistrationId)(FakeRequest().withBody[JsValue](buildMetadataResponseJson(cc = "Guardian")))

      status(result) mustBe FORBIDDEN
    }
  }

  "calling updateLastSignedIn" should {
    "return a NotFound if no data is found" in new Setup {
      stubSuccessfulLogin

      val result: Future[Result] = controller.updateLastSignedIn(testRegistrationId)(FakeRequest().withBody[JsValue](Json.parse("""{"DateTime": "2010-05-12"}""")))

      status(result) mustBe NOT_FOUND
    }

    "return a BadRequest if an invalid json is supplied" in new Setup {
      stubSuccessfulLogin
      await(insertMetadata())

      val result: Future[Result] = controller.updateLastSignedIn(testRegistrationId)(FakeRequest().withBody[JsValue](Json.parse("""{"DateTime": "ACE12"}""")))

      status(result) mustBe BAD_REQUEST
    }

    "return a 200 with a response body" in new Setup {
      stubSuccessfulLogin
      await(insertMetadata())

      val result: Future[Result] = controller.updateLastSignedIn(testRegistrationId)(FakeRequest().withBody[JsValue](Json.toJson(validNewDateTime)))

      status(result) mustBe OK
      val json: Option[Metadata] = await(getMetadata())
      json.isEmpty mustBe false
      json.get.lastSignedIn.toDateTime.dayOfMonth().get() mustBe validNewDateTime.getDayOfMonth
      json.get.lastSignedIn.toDateTime.monthOfYear().get() mustBe validNewDateTime.getMonthValue
      json.get.lastSignedIn.toDateTime.year().get() mustBe validNewDateTime.getYear
    }

    "return a Forbidden if the user is not LoggedIn" in new Setup {
      stubNotLoggedIn

      val result: Future[Result] = controller.updateLastSignedIn(testRegistrationId)(FakeRequest().withBody[JsValue](Json.toJson(validNewDateTime)))

      status(result) mustBe FORBIDDEN
    }
  }
}
