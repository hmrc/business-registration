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


object TradingName {
  def now: DateTime = DateTime.now(DateTimeZone.UTC)

  def format: Format[String] = new Format[String] {
    override def writes(str: String): JsValue = Json.obj(
      "tradingName" -> str
    )

    override def reads(json: JsValue): JsResult[String] = json.\("tradingName").validate[String]
  }

  def mongoWrites(registrationID: String, internalID: String, dateTime: DateTime = now): OWrites[String] = new OWrites[String] {
    override def writes(tradingName: String): JsObject = {
      Json.obj(
        "_id" -> registrationID,
        "internalId" -> internalID,
        "lastUpdated" -> Json.toJson(dateTime)(ReactiveMongoFormats.dateTimeWrite),
        "tradingName" -> tradingName
      )
    }
  }

  val mongoTradingNameReads: Reads[String] = new Reads[String] {
    def reads(json: JsValue): JsResult[String] = {
      (json \ "tradingName").validate[String]
    }
  }

  val mongoInternalIdReads: Reads[String] = new Reads[String] {
    def reads(json: JsValue): JsResult[String] = {
      (json \ "internalId").validate[String]
    }
  }
}
