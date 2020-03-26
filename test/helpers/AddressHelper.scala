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

package helpers

import play.api.libs.json.{JsObject, JsValue, Json}

import scala.language.implicitConversions
import scala.util.Random

trait AddressHelper {

  implicit def toJsObject(v: JsValue): JsObject = v.as[JsObject]

  def generateOID: String = {
    val alpha = "abcdef123456789"
    (1 to 24).map(x => alpha(Random.nextInt.abs % alpha.length)).mkString
  }

  def buildAddressJson(regId: String, withOid: Boolean = true, invalid: Boolean = false, different: Boolean = false): JsObject = {

    val rId = if (different) generateOID.take(3) else regId

    val addressJson = Json.parse(
      s"""
         |{
         |  "addressLine1" : "testAddressLine1-$rId",
         |  "addressLine2" : "testAddressLine2-$rId",
         |  "addressLine3" : "testAddressLine3-$rId",
         |  "addressLine4" : "testAddressLine4-$rId",
         |  "postcode" : "testPostcode-$rId",
         |  "country" : "testCountry-$rId"
         |}
    """.stripMargin).as[JsObject] ++
      (if (withOid) Json.parse(s"""{"_id" : {"$$oid" : "$generateOID"}}""") else Json.obj())

    if (invalid) addressJson.-("postcode").-("country") else addressJson
  }

  case class FetchOptions(regId: String, withOid: Boolean = true, invalid: Boolean = false, different: Boolean = false)

  def buildFetchedAddressJson(options: Seq[FetchOptions]): JsObject = {
    Json.obj("addresses" -> Json.toJson((options map (option => buildAddressJson(
      option.regId, withOid = option.withOid, invalid = option.invalid, different = option.different
    )))
      .map(_.-("_id").-("registration_id"))))
  }
}
