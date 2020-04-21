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

package controllers.prePop

import auth.AuthorisationResource
import helpers.SCRSSpec
import mocks.AuthMocks
import models.prepop.{ContactDetails, PermissionDenied}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import repositories.MetadataMongoRepository
import repositories.prepop.ContactDetailsRepository

import scala.concurrent.Future

class ContactDetailsControllerSpec extends SCRSSpec with AuthMocks {

  val mockAuthorisationResource: AuthorisationResource = mock[AuthorisationResource]
  val mockContactDetailsRepo: ContactDetailsRepository = mock[ContactDetailsRepository]
  val mockMetadataMongoRepository: MetadataMongoRepository = mock[MetadataMongoRepository]

  class Setup {
    val controller = new ContactDetailsController(
      mockMetadataMongoRepository,
      mockContactDetailsRepo,
      mockAuthConnector,
      stubControllerComponents()
    )
  }

  val validContactDetails: ContactDetails = ContactDetails(
    firstName = Some("first"),
    middleName = Some("middle"),
    surname = Some("last"),
    email = Some("one@two.three"),
    telephoneNumber = Some("1234567898765"),
    mobileNumber = Some("9876543321324")
  )

  val validContactDetailsJson: JsValue = Json.parse(
    """
      |{
      | "firstName": "first",
      | "middleName": "middle",
      | "surname": "last",
      | "email": "one@two.three",
      | "telephoneNumber": "1234567898765",
      | "mobileNumber": "9876543321324"
      |}
    """.stripMargin)

  val validUpdateContactDetailsJson: JsValue = Json.parse(
    """
      |{
      | "middleName": "newMiddle",
      | "email": "new@email.com"
      |}
    """.stripMargin)

  val invalidUpdateContactDetailsJson: JsValue = Json.parse(
    """
      |{
      | "middleName": 12343,
      | "email": "new@email.com"
      |}
    """.stripMargin)

  "getContactDetails" should {
    "return OK if all goes well and contact details are found" in new Setup {
      mockSuccessfulAuthentication
      when(mockContactDetailsRepo.getContactDetails(any(), any())(any()))
        .thenReturn(Future.successful(Some(validContactDetails)))

      val result: Future[Result] = controller.getContactDetails("12345")(FakeRequest())

      status(result) mustBe OK
      bodyAsJson(result) mustBe validContactDetailsJson
    }

    "return NotFound if no ContactDetails found" in new Setup {
      mockSuccessfulAuthentication
      when(mockContactDetailsRepo.getContactDetails(any(), any())(any()))
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.getContactDetails("12345")(FakeRequest())

      status(result) mustBe NOT_FOUND
    }

    "return forbidden if the the user is not authorised to get the record" in new Setup {
      mockSuccessfulAuthentication
      when(mockContactDetailsRepo.getContactDetails(any(), any())(any()))
        .thenReturn(Future.failed(PermissionDenied("12345", "12345")))

      val result: Future[Result] = controller.getContactDetails("12345")(FakeRequest())

      status(result) mustBe FORBIDDEN
    }

    "return a forbidden if the user is not logged in" in new Setup {
      mockNotLoggedIn

      val result: Future[Result] = controller.getContactDetails("12345")(FakeRequest())

      status(result) mustBe FORBIDDEN
    }
  }

  "updateContactDetails" should {
    "return OK if all goes well and contact details are found" in new Setup {
      mockSuccessfulAuthentication
      when(mockContactDetailsRepo.upsertContactDetails(any(), any(), any())(any()))
        .thenReturn(Future.successful(Some(validContactDetails)))

      val result: Future[Result] = controller.insertUpdateContactDetails("12345")(FakeRequest().withBody[JsValue](validUpdateContactDetailsJson))

      status(result) mustBe OK
    }

    "return NotFound if no ContactDetails found" in new Setup {
      mockSuccessfulAuthentication
      when(mockContactDetailsRepo.upsertContactDetails(any(), any(), any())(any()))
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.insertUpdateContactDetails("12345")(FakeRequest().withBody[JsValue](validContactDetailsJson))

      status(result) mustBe NOT_FOUND
    }

    "return forbidden if the the user is not authorised to get the record" in new Setup {
      mockSuccessfulAuthentication
      when(mockContactDetailsRepo.upsertContactDetails(any(), any(), any())(any()))
        .thenReturn(Future.failed(PermissionDenied("12345", "12345")))

      val result: Future[Result] = controller.insertUpdateContactDetails("12345")(FakeRequest().withBody(validContactDetailsJson))

      status(result) mustBe FORBIDDEN
    }

    "return BadRequest if an invalid json is supplied" in new Setup {
      mockSuccessfulAuthentication

      val result: Future[Result] = controller.insertUpdateContactDetails("12345")(FakeRequest().withBody[JsValue](invalidUpdateContactDetailsJson))

      status(result) mustBe BAD_REQUEST
    }

    "return a forbidden if the user is not logged in" in new Setup {
      mockNotLoggedIn

      val result: Future[Result] = controller.insertUpdateContactDetails("12345")(FakeRequest().withBody[JsValue](validUpdateContactDetailsJson))

      status(result) mustBe FORBIDDEN
    }
  }
}
