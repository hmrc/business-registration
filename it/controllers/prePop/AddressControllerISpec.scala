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

package controllers.prePop

import itutil.IntegrationSpecBase
import org.mongodb.scala.model.Filters
import org.mongodb.scala.result.DeleteResult
import play.api.libs.json._
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.prepop.AddressRepository
import services.prepop.AddressService
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddressControllerISpec extends IntegrationSpecBase {

  class Setup {
    lazy val addressService: AddressService = app.injector.instanceOf[AddressService]
    lazy val addressRepository: AddressRepository = app.injector.instanceOf[AddressRepository]
    lazy val authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

    val controller = new AddressController(addressService, addressRepository, authConnector, stubControllerComponents())

    def dropAddress(regId: String = testRegistrationId): DeleteResult =
      await(addressRepository.collection.deleteOne(Filters.equal("registration_id", regId)).toFuture())

    def insertAddress(address: JsObject = validAddressJson ++ validIdsJson): Boolean =
      await(addressRepository.insertAddress(address))

    def getAddresses(regId: String = testRegistrationId): Option[JsObject] = await(addressRepository.fetchAddresses(regId))

    dropAddress()
  }

  val validAddressJson: JsObject = Json.parse(
    s"""
       |{
       | "addressLine1": "woooop",
       | "postcode": "weeee",
       | "country" : "noon12"
       |}
    """.stripMargin).as[JsObject]

  val validIdsJson: JsObject = Json.obj("registration_id" -> testRegistrationId, "internal_id" -> testInternalId)

  val invalidAddressJson: JsObject = Json.parse(
    """
      |{
      | "postcode": "weeee",
      | "country" : "noon12"
      |}
    """.stripMargin).as[JsObject]

  val validUpdateAddressJson: JsValue = Json.parse(
    """
      |{
      | "addressLine1": "newLine1",
      |  "postcode": "newPostCode",
      | "country" : "newCountry"
      |}
    """.stripMargin)


  "calling getAddressDetails" should {
    "return a NotFound if no data is found" in new Setup {
      stubSuccessfulLogin

      val result: Future[Result] = controller.fetchAddresses(testRegistrationId)(FakeRequest().withHeaders("Authorization" -> "Bearer123"))

      status(result) mustBe NOT_FOUND
    }

    "return a 200 with a response body" in new Setup {
      stubSuccessfulLogin
      insertAddress()

      val result: Future[Result] = controller.fetchAddresses(testRegistrationId)(FakeRequest().withHeaders("Authorization" -> "Bearer123"))

      status(result) mustBe OK
    }

    "return a Forbidden if the user is not LoggedIn" in new Setup {
      stubNotLoggedIn

      val result: Future[Result] = controller.fetchAddresses(testRegistrationId)(FakeRequest().withHeaders("Authorization" -> "Bearer123"))

      status(result) mustBe FORBIDDEN
    }
  }

  "calling updateAddress" should {
    "return a BadRequest if an invalid json is supplied" in new Setup {
      stubSuccessfulLogin
      insertAddress()

      val result: Future[Result] = controller.updateAddress(testRegistrationId)(FakeRequest().withBody[JsValue](invalidAddressJson).withHeaders("Authorization" -> "Bearer123"))

      status(result) mustBe BAD_REQUEST
    }

    "return a 200 with a response body" in new Setup {
      stubSuccessfulLogin
      insertAddress()

      val result: Future[Result] = controller.updateAddress(testRegistrationId)(FakeRequest().withBody[JsValue](validUpdateAddressJson).withHeaders("Authorization" -> "Bearer123"))

      status(result) mustBe OK
      val json: Option[JsObject] = getAddresses()
      json.isEmpty mustBe false
      ((json.get \ "addresses").as[Seq[JsValue]].head \ "addressLine1").asOpt[String] mustBe Some("newLine1")
    }

    "return a Forbidden if the user is not LoggedIn" in new Setup {
      stubNotLoggedIn

      val result: Future[Result] = controller.updateAddress(testRegistrationId)(FakeRequest().withBody[JsValue](validUpdateAddressJson).withHeaders("Authorization" -> "Bearer123"))
      status(result) mustBe FORBIDDEN
    }
  }
}
