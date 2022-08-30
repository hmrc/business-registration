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

package services.prepop

import helpers.AddressHelper
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.Helpers._
import repositories.prepop.AddressRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddressServiceSpec extends PlaySpec with MockitoSugar with AddressHelper {

  val mockAddressesRepository: AddressRepository = mock[AddressRepository]

  class Setup {
    val service = new AddressService(mockAddressesRepository)

    def mockFetchAddresses(toReturn: Option[JsObject]): OngoingStubbing[Future[Option[JsObject]]] =
      when(mockAddressesRepository.fetchAddresses(any())(any())).thenReturn(Future.successful(toReturn))

    def mockUpdateAddress(successful: Boolean): OngoingStubbing[Future[Boolean]] =
      when(mockAddressesRepository.insertAddress(any())(any())).thenReturn(Future.successful(successful))

    def mockUpdateTTL(successful: Boolean): OngoingStubbing[Future[Boolean]] =
      when(mockAddressesRepository.updateAddress(any(), any())(any())).thenReturn(Future.successful(successful))
  }

  val regId = "reg-12345"

  "fetchAddresses" should {
    "return whatever is fetched from the repository" in new Setup {
      val returnedAddressJson: JsObject = buildFetchedAddressJson(Seq(FetchOptions(regId, withOid = false)))

      mockFetchAddresses(Some(returnedAddressJson))

      val result: Option[JsObject] = await(service.fetchAddresses(regId))

      result mustBe Some(returnedAddressJson)
    }
  }

  "updateAddress" should {
    "return true if the address doesn't already exist and the update is successful" in new Setup {
      val suppliedAddressJson: JsObject = buildAddressJson(regId, withOid = false)

      mockFetchAddresses(None)
      mockUpdateAddress(successful = true)

      val result: Boolean = await(service.updateAddress(regId, suppliedAddressJson))

      result mustBe true
    }

    "return false if the address doesn't already exist but the update was unsuccessful" in new Setup {
      val suppliedAddressJson: JsObject = buildAddressJson(regId, withOid = false)

      mockFetchAddresses(None)
      mockUpdateAddress(successful = false)

      val result: Boolean = await(service.updateAddress(regId, suppliedAddressJson))

      result mustBe false
    }

    "return true if the address already exists" in new Setup {
      val suppliedAddressJson: JsObject = buildAddressJson(regId, withOid = false)
      val returnedAddressJson: JsObject = buildFetchedAddressJson(Seq(FetchOptions(regId)))

      mockFetchAddresses(Some(returnedAddressJson))
      mockUpdateTTL(successful = true)

      val result: Boolean = await(service.updateAddress(regId, suppliedAddressJson))

      result mustBe true
    }
  }

  "addressExists" should {
    "return true" when {
      "the returned address array contains the supplied address when only 1 address is returned" in new Setup {
        val suppliedAddressJson: JsObject = buildAddressJson(regId)
        val returnedAddressJson: JsObject = buildFetchedAddressJson(Seq(FetchOptions(regId)))

        mockFetchAddresses(Some(returnedAddressJson))

        await(service.addressExists(regId, suppliedAddressJson)) mustBe true
      }

      "the returned address array contains the same supplied address but contains different cases" in new Setup {
        val suppliedAddressJson: JsValue = Json.parse(
          s"""{
             |  "addressLine1" : "testAddressLine1",
             |  "addressLine2" : "testAddressLine2",
             |  "addressLine3" : "testAddressLine3",
             |  "addressLine4" : "testAddressLine4",
             |  "postcode" : "testPostcode",
             |  "country" : "testCountry"
             |}
             |""".stripMargin)

        val returnedAddressJson: JsValue = Json.parse(
          s"""{
             |"addresses" : [
             |  {
             |    "addressLine1" : "TESTADDRESSLINE1",
             |    "addressLine2" : "testAddressLine2",
             |    "addressLine3" : "testAddressLine3",
             |    "addressLine4" : "testAddressLine4",
             |    "postcode" : "TESTPOSTCODE",
             |    "country" : "TESTCOUNTRY"
             |  }
             |]}
             |""".stripMargin)

        mockFetchAddresses(Some(returnedAddressJson))

        await(service.addressExists(regId, suppliedAddressJson)) mustBe true
      }

      "the returned address array contains the supplied address when multiple addresses are returned" in new Setup {
        val suppliedAddressJson: JsObject = buildAddressJson(regId, withOid = false)
        val returnedAddressJson: JsObject = buildFetchedAddressJson(Seq(FetchOptions(regId), FetchOptions(regId, different = true)))

        mockFetchAddresses(Some(returnedAddressJson))

        await(service.addressExists(regId, suppliedAddressJson)) mustBe true
      }
    }

    "return false" when {
      "the returned address array does not match the supplied address when only 1 address is returned" in new Setup {
        val suppliedAddressJson: JsObject = buildAddressJson(regId)
        val returnedAddressJson: JsObject = buildFetchedAddressJson(Seq(FetchOptions(regId, different = true)))

        mockFetchAddresses(Some(returnedAddressJson))

        await(service.addressExists(regId, suppliedAddressJson)) mustBe false
      }

      "the returned address array does not match the supplied address when many addresses are returned" in new Setup {
        val suppliedAddressJson: JsObject = buildAddressJson(regId)
        val returnedAddressJson: JsObject = buildFetchedAddressJson(Seq(FetchOptions(regId, different = true), FetchOptions(regId, different = true)))

        mockFetchAddresses(Some(returnedAddressJson))

        await(service.addressExists(regId, suppliedAddressJson)) mustBe false
      }
    }
  }
}
