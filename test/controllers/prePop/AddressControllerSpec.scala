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

import helpers.{AddressHelper, SCRSSpec}
import mocks.AuthMocks
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Results.Ok
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import repositories.prepop.AddressRepository
import services.prepop.AddressService

import scala.concurrent.Future

class AddressControllerSpec extends SCRSSpec with MockitoSugar with AddressHelper with AuthMocks {

  val mockAddressService: AddressService = mock[AddressService]
  val mockAddressRepository: AddressRepository = mock[AddressRepository]

  class Setup {
    val controller = new AddressController(
      mockAddressService,
      mockAddressRepository,
      mockAuthConnector,
      stubControllerComponents()
    )

    def mockGetInternalIds(internalIds: String*): OngoingStubbing[Future[Seq[String]]] =
      when(mockAddressRepository.getInternalIds(any())(any())).thenReturn(Future.successful(internalIds))

    def mockFetchAddress(address: Option[JsObject]): OngoingStubbing[Future[Option[JsObject]]] =
      when(mockAddressService.fetchAddresses(any())(any())).thenReturn(Future.successful(address))

    def mockUpdateAddress(succeeded: Boolean): OngoingStubbing[Future[Boolean]] =
      when(mockAddressService.updateAddress(any(), any())(any())).thenReturn(Future.successful(succeeded))

  }

  val regId = "reg-12345"

  "fetchAddresses" should {
    "return a 200 and the fetched address as the json body if the user is authorised" in new Setup {
      val fetchedAddress: JsObject = buildFetchedAddressJson(Seq(FetchOptions(regId)))
      mockSuccessfulAuthorisation(mockAddressRepository, regId)
      mockFetchAddress(Some(fetchedAddress))

      val result: Future[Result] = controller.fetchAddresses(regId)(FakeRequest())

      status(result) mustBe 200
      bodyAsJson(result) mustBe fetchedAddress
    }

    "return a 404 when no address can be found for the provided registration id" in new Setup {
      mockSuccessfulAuthorisation(mockAddressRepository, regId)
      mockFetchAddress(None)

      val result: Future[Result] = controller.fetchAddresses(regId)(FakeRequest())

      status(result) mustBe 404
    }
  }

  "updateAddress" should {
    "return a 200" in new Setup {
      val jsonBody: JsObject = buildAddressJson(regId, withOid = false)
      val request: FakeRequest[JsValue] = FakeRequest().withBody[JsValue](jsonBody)

      mockSuccessfulAuthentication
      mockGetInternalIds(validUserIds.internalId)
      mockUpdateAddress(succeeded = true)

      val result: Future[Result] = controller.updateAddress(regId)(request)

      status(result) mustBe 200
    }

    "return a 500 if there was a problem updating teh address" in new Setup {
      val jsonBody: JsObject = buildAddressJson(regId, withOid = false)
      val request: FakeRequest[JsValue] = FakeRequest().withBody[JsValue](jsonBody)

      mockSuccessfulAuthentication
      mockGetInternalIds(validUserIds.internalId)
      mockUpdateAddress(succeeded = false)

      val result: Future[Result] = controller.updateAddress(regId)(request)

      status(result) mustBe 500
    }
  }

  "authenticatedToUpdate" should {
    "execute the body if authenticated to update" in new Setup {
      val address: JsObject = buildAddressJson(regId, withOid = false)
      val body: JsObject => Future[Results.Status] = (_: JsObject) => Future.successful(Ok)

      mockSuccessfulAuthentication
      mockGetInternalIds(validUserIds.internalId)

      val result: Future[Result] = controller.authenticatedToUpdate(regId, address)(body)

      status(result) mustBe 200
    }

    "Append the internal ID to the supplied address json on successful authentication" in new Setup {
      val address: JsObject = buildAddressJson(regId, withOid = false)
      val body: JsObject => Future[Results.Status] = (_: JsObject) => Future.successful(Ok)

      val internalIdJson: JsObject = Json.obj("internal_id" -> validUserIds.internalId)
      val regIdJson: JsObject = Json.obj("registration_id" -> regId)

      mockSuccessfulAuthentication
      mockGetInternalIds(validUserIds.internalId)

      await(controller.authenticatedToUpdate(regId, address) {
        addressJson =>
          addressJson mustBe (address ++ internalIdJson ++ regIdJson)
          body(addressJson)
      })
    }

    "return a 403 if the internal id fetched from the address repository is not equal to the users'" in new Setup {
      val address: JsObject = buildAddressJson(regId, withOid = false)
      val body: JsObject => Future[Results.Status] = (_: JsObject) => Future.successful(Ok)

      mockSuccessfulAuthentication
      mockGetInternalIds("unmatchedInternalID")

      val result: Future[Result] = controller.authenticatedToUpdate(regId, address)(body)

      status(result) mustBe 403
    }

    "execute the body when there are no documents associated with the registration ID" in new Setup {
      val address: JsObject = buildAddressJson(regId, withOid = false)
      val body: JsObject => Future[Results.Status] = (_: JsObject) => Future.successful(Ok)

      mockSuccessfulAuthentication
      mockGetInternalIds()

      val result: Future[Result] = controller.authenticatedToUpdate(regId, address)(body)

      status(result) mustBe 200
    }
  }

  "ifAddressValid" should {
    "execute the body if the supplied Address is valid" in new Setup {
      val address: JsObject = buildAddressJson(regId, withOid = false)
      val body: Future[Results.Status] = Future.successful(Results.Ok)

      val result: Future[Result] = controller.ifAddressValid(address)(body)
      status(result) mustBe 200
    }

    "return a 400 if the supplied Address is invalid" in new Setup {
      val address: JsObject = buildAddressJson(regId, withOid = false, invalid = true)
      val body: Future[Results.Status] = Future.successful(Results.Ok)

      val result: Future[Result] = controller.ifAddressValid(address)(body)
      status(result) mustBe 400
    }

    "return a 400 if the supplied json can't be marshaled into the Address case class" in new Setup {
      val address: JsValue = Json.parse("""{"will-not":"marshal"}""")
      val body: Future[Results.Status] = Future.successful(Results.Ok)

      val result: Future[Result] = controller.ifAddressValid(address)(body)
      status(result) mustBe 400
    }
  }
}
