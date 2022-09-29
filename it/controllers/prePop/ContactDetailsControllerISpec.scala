/*
 * Copyright 2022 HM Revenue & Customs
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
import models.prepop.ContactDetails
import org.mongodb.scala.model.Filters
import org.mongodb.scala.result.DeleteResult
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.MetadataMongoRepository
import repositories.prepop.ContactDetailsRepository
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ContactDetailsControllerISpec extends IntegrationSpecBase with MetadataFixtures {

  class Setup {
    lazy val contactDetailsRepository: ContactDetailsRepository = app.injector.instanceOf[ContactDetailsRepository]
    lazy val metadataMongoRepository: MetadataMongoRepository = app.injector.instanceOf[MetadataMongoRepository]
    lazy val authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

    val controller = new ContactDetailsController(metadataMongoRepository, contactDetailsRepository, authConnector, stubControllerComponents())

    def dropContactDetails(internalId: String = testInternalId, regId: String = testRegistrationId): DeleteResult =
      await(contactDetailsRepository.collection.deleteOne(
        Filters.and(Filters.equal("_id", regId), Filters.equal("InternalID", internalId))
      ).toFuture())

    def insertContactDetails(regId: String = testRegistrationId, intId: String = testInternalId, contact: ContactDetails = validContactDetails): Option[ContactDetails] =
      await(contactDetailsRepository.upsertContactDetails(regId, intId, contact))

    def getContactDetails(regId: String = testRegistrationId, intId: String = testInternalId): Option[ContactDetails] =
      await(contactDetailsRepository.getContactDetails(regId, intId))

    dropContactDetails()
  }

  val validContactDetails: ContactDetails = ContactDetails(
    firstName = Some("first"),
    middleName = Some("middle"),
    surname = Some("last"),
    email = Some("who@is.this"),
    telephoneNumber = Some("1234567896576"),
    mobileNumber = Some("987382728372")
  )

  val validUpdateContactJson: JsValue = Json.parse(
    """
      |{
      | "firstName"  : "first",
      | "middleName" : "newMiddle",
      | "surname"    : "last"
      |}
    """.stripMargin)

  val invalidUpdateContactJson: JsValue = Json.parse(
    """
      |{
      | "middleName" : true
      |}
    """.stripMargin)

  "calling getContactDetails" should {
    "return a NotFound if no data is found" in new Setup {
      stubSuccessfulLogin

      val result: Future[Result] = controller.getContactDetails(testRegistrationId)(FakeRequest().withHeaders("Authorization" -> "Bearer123"))

      status(result) mustBe NOT_FOUND
    }

    "return a 200 with a response body" in new Setup {
      stubSuccessfulLogin
      insertContactDetails()

      val result: Future[Result] = controller.getContactDetails(testRegistrationId)(FakeRequest().withHeaders("Authorization" -> "Bearer123"))

      status(result) mustBe OK
    }

    "return a Forbidden if the user is not LoggedIn" in new Setup {
      stubNotLoggedIn

      val result: Future[Result] = controller.getContactDetails(testRegistrationId)(FakeRequest().withHeaders("Authorization" -> "Bearer123"))

      status(result) mustBe FORBIDDEN
    }
  }

  "calling insertUpdateContactDetails" should {
    "return a BadRequest if an invalid json is supplied" in new Setup {
      stubSuccessfulLogin
      insertContactDetails()

      val result: Future[Result] = controller.insertUpdateContactDetails(testRegistrationId)(FakeRequest().withBody[JsValue](invalidUpdateContactJson).withHeaders("Authorization" -> "Bearer123"))

      status(result) mustBe BAD_REQUEST
    }

    "return a 200 with a response body" in new Setup {
      stubSuccessfulLogin
      insertContactDetails()

      val result: Future[Result] = controller.insertUpdateContactDetails(testRegistrationId)(FakeRequest().withBody[JsValue](validUpdateContactJson).withHeaders("Authorization" -> "Bearer123"))

      status(result) mustBe OK
      val json: Option[ContactDetails] = getContactDetails()
      json.isEmpty mustBe false
      json.get.middleName mustBe Some("newMiddle")
    }

    "return a Forbidden if the user is not LoggedIn" in new Setup {
      stubNotLoggedIn

      val result: Future[Result] = controller.insertUpdateContactDetails(testRegistrationId)(FakeRequest().withBody[JsValue](Json.toJson(validNewDate)).withHeaders("Authorization" -> "Bearer123"))

      status(result) mustBe FORBIDDEN
    }
  }
}
