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

package models.prepop

import org.scalatestplus.play.PlaySpec

class AddressSpec extends PlaySpec {

  val addressLine1: String = "testAddressLine1"
  val postcode: String = "testPostcode"
  val country: String = "testCountry"

  val address: Address = Address(addressLine1, Some(postcode), Some(country))
  val addressNoPostcode: Address = Address(addressLine1, None, Some(country))
  val addressNoPostcodeOrCountry: Address = Address(addressLine1, None, None)

  "sameAs" should {
    "return true" when {
      "address line 1 and postcode are the same" in {
        val otherAddress = Address(addressLine1, Some(postcode), None)

        address.sameAs(otherAddress) mustBe true
      }

      "address line 1 and postcode are the same but have different cases" in {
        val otherAddress = Address(addressLine1.toUpperCase, Some(postcode.toUpperCase), None)

        address.sameAs(otherAddress) mustBe true
      }

      "address line 1 and country are the same but postcode is not set in either address" in {
        val otherAddress = Address(addressLine1, None, Some(country))

        addressNoPostcode.sameAs(otherAddress) mustBe true
      }
    }

    "return false" when {
      "address line 1 is the same but country and postcode are not set in either address" in {
        val otherAddress = Address(addressLine1, None, None)

        addressNoPostcodeOrCountry.sameAs(otherAddress) mustBe false
      }

      "address line 1 is not the same" in {
        val otherAddress = Address("otherAddressLine1", None, None)

        address.sameAs(otherAddress) mustBe false
      }

      "address line 1 is the same but postcode is not" in {
        val otherAddress = Address(addressLine1, Some("otherPostcode"), None)

        address.sameAs(otherAddress) mustBe false
      }

      "address line 1 and country is the same but postcode is not" in {
        val otherAddress = Address(addressLine1, Some("otherPostcode"), Some(country))

        address.sameAs(otherAddress) mustBe false
      }

      "address line 1 and country are the same but other postcode doesn't exist" in {
        val otherAddress = Address(addressLine1, None, Some(country))

        address.sameAs(otherAddress) mustBe false
      }
    }
  }
}
