/*
 * Copyright 2017 HM Revenue & Customs
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
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.{JsObject, Json}
import repositories.prepop.AddressMongoRepository
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any}

import scala.concurrent.Future

class AddressServiceSpec extends UnitSpec with MockitoSugar with AddressHelper {

  val mockAddressesRepository = mock[AddressMongoRepository]

  class Setup {
    val service = new AddressService {
      override val repository = mockAddressesRepository
    }

    def mockFetchAddresses(toReturn: Option[JsObject]) = when(mockAddressesRepository.fetchAddresses(any())).thenReturn(Future.successful(toReturn))
    def mockUpdateAddress(successful: Boolean) = when(mockAddressesRepository.insertAddress(any(), any())).thenReturn(Future.successful(successful))
    def mockUpdateTTL(successful: Boolean) = when(mockAddressesRepository.updateAddress(any(), any())).thenReturn(Future.successful(successful))
  }

  val regId = "reg-12345"

  "fetchAddresses" should {

    "return whatever is fetched from the repository" in new Setup {
      val returnedAddressJson = buildFetchedAddressJson(Seq(FetchOptions(regId, withOid = false)))

      mockFetchAddresses(Some(returnedAddressJson))

      val result = await(service.fetchAddresses(regId))

      result shouldBe Some(returnedAddressJson)
    }
  }

  "updateAddress" should {

    "return true if the address doesn't already exist and the update is successful" in new Setup {
      val suppliedAddressJson = buildAddressJson(regId, withOid = false)

      mockFetchAddresses(None)
      mockUpdateAddress(successful = true)

      val result = await(service.updateAddress(regId, suppliedAddressJson))

      result shouldBe true
    }

    "return false if the address doesn't already exist but the update was unsuccessful" in new Setup {
      val suppliedAddressJson = buildAddressJson(regId, withOid = false)

      mockFetchAddresses(None)
      mockUpdateAddress(successful = false)

      val result = await(service.updateAddress(regId, suppliedAddressJson))

      result shouldBe false
    }

    "return true if the address already exists" in new Setup {
      val suppliedAddressJson = buildAddressJson(regId, withOid = false)
      val returnedAddressJson = buildFetchedAddressJson(Seq(FetchOptions(regId)))

      mockFetchAddresses(Some(returnedAddressJson))
      mockUpdateTTL(successful = true)

      val result = await(service.updateAddress(regId, suppliedAddressJson))

      result shouldBe true
    }
  }

  "addressExists" should {

    "return true" when {

      "the returned address array contains the supplied address when only 1 address is returned" in new Setup {
        val suppliedAddressJson = buildAddressJson(regId)
        val returnedAddressJson = buildFetchedAddressJson(Seq(FetchOptions(regId)))

        mockFetchAddresses(Some(returnedAddressJson))

        await(service.addressExists(regId, suppliedAddressJson)) shouldBe true
      }

      "the returned address array contains the supplied address when multiple addresses are returned" in new Setup {
        val suppliedAddressJson = buildAddressJson(regId, withOid = false)
        val returnedAddressJson = buildFetchedAddressJson(Seq(FetchOptions(regId), FetchOptions(regId, different = true)))

        mockFetchAddresses(Some(returnedAddressJson))

        await(service.addressExists(regId, suppliedAddressJson)) shouldBe true
      }
    }

    "return false" when {

      "the returned address array does not match the supplied address when only 1 address is returned" in new Setup {
        val suppliedAddressJson = buildAddressJson(regId)
        val returnedAddressJson = buildFetchedAddressJson(Seq(FetchOptions(regId, different = true)))

        mockFetchAddresses(Some(returnedAddressJson))

        await(service.addressExists(regId, suppliedAddressJson)) shouldBe false
      }

      "the returned address array does not match the supplied address when many addresses are returned" in new Setup {
        val suppliedAddressJson = buildAddressJson(regId)
        val returnedAddressJson = buildFetchedAddressJson(Seq(FetchOptions(regId, different = true), FetchOptions(regId, different = true)))

        mockFetchAddresses(Some(returnedAddressJson))

        await(service.addressExists(regId, suppliedAddressJson)) shouldBe false
      }
    }
  }
}
