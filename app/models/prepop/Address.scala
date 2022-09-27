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

package models.prepop

import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import scala.util.{Failure, Success, Try}

object Address {

  val mongoWrites: OWrites[JsObject] = (o: JsObject) =>
    o.deepMerge(Json.obj("lastUpdated" -> Json.toJson(Instant.now())(MongoJavatimeFormats.instantWrites)))

  val writes: OWrites[JsObject] = (o: JsObject) => o

  val reads: Reads[JsObject] = (json: JsValue) =>
    Try(json.as[JsObject]) match {
      case Success(v) => JsSuccess(v)
      case Failure(e) => JsError(e.getMessage)
    }

  val format: Format[JsObject] = Format(reads, writes)

  val addressReads: Reads[Address] = (json: JsValue) => {
    val addressLine1 = (json \ "addressLine1").as[String]
    val postcode = (json \ "postcode").asOpt[String]
    val country = (json \ "country").asOpt[String]

    JsSuccess(Address(addressLine1, postcode, country))
  }

  val listReads: Reads[Seq[Address]] = (json: JsValue) =>
    JsSuccess((json \ "addresses").as[Seq[JsObject]].map(_.as[Address](Address.addressReads)))
}

case class Address(addressLine1: String,
                   postcode: Option[String],
                   country: Option[String]) {

  def isValid: Boolean = postcode.isDefined || country.isDefined

  def sameAs(that: Address): Boolean = {
    this.addressLine1.toLowerCase == that.addressLine1.toLowerCase && {
      (this.postcode, that.postcode, this.country, that.country) match {
        case (Some(p1), Some(p2), _, _) if p1.toLowerCase == p2.toLowerCase => true
        case (None, None, Some(c1), Some(c2)) if c1.toLowerCase == c2.toLowerCase => true
        case _ => false
      }
    }
  }
}
