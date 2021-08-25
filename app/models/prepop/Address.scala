/*
 * Copyright 2021 HM Revenue & Customs
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

import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json._
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.util.{Failure, Success, Try}

object Address {

  val mongoWrites: OWrites[JsObject] = new OWrites[JsObject] {
    override def writes(o: JsObject): JsObject = {
      o.deepMerge(Json.obj("lastUpdated" -> Json.toJson(DateTime.now(DateTimeZone.UTC))(ReactiveMongoFormats.dateTimeWrite)))
    }
  }

  val writes: OWrites[JsObject] = new OWrites[JsObject] {
    override def writes(o: JsObject): JsObject = o
  }

  val reads: Reads[JsObject] = new Reads[JsObject] {
    override def reads(json: JsValue): JsResult[JsObject] = {
      Try(json.as[JsObject]) match {
        case Success(v) => JsSuccess(v)
        case Failure(e) => JsError(e.getMessage)
      }
    }
  }

  val format: Format[JsObject] = Format(reads, writes)

  val addressReads: Reads[Address] = new Reads[Address] {
    override def reads(json: JsValue): JsResult[Address] = {
      val addressLine1 = (json \ "addressLine1").as[String]
      val postcode = (json \ "postcode").asOpt[String]
      val country = (json \ "country").asOpt[String]

      JsSuccess(Address(addressLine1, postcode, country))
    }
  }

  val listReads: Reads[Seq[Address]] = new Reads[Seq[Address]] {
    override def reads(json: JsValue): JsResult[Seq[Address]] = {
      JsSuccess((json \ "addresses").as[Seq[JsObject]].map(_.as[Address](Address.addressReads)))
    }
  }
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
