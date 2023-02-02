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

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class MongoTradingName(registrationID: String,
                            internalID: String,
                            tradingName: String,
                            dateTime: Instant = Instant.now())

object MongoTradingName {

  val mongoReads: Reads[MongoTradingName] = (
    (__ \ "_id").read[String] and
      (__ \ "internalId").read[String] and
      (__ \ "tradingName").read[String] and
      (__ \ "lastUpdated").read[Instant](MongoJavatimeFormats.instantReads)
    )(MongoTradingName.apply _)

  val mongoWrites: OWrites[MongoTradingName] = (tn: MongoTradingName) =>
    Json.obj(
      "_id" -> tn.registrationID,
      "internalId" -> tn.internalID,
      "tradingName" -> tn.tradingName,
      "lastUpdated" -> Json.toJson(tn.dateTime)(MongoJavatimeFormats.instantWrites)
    )

  val mongoFormat: Format[MongoTradingName] = Format(mongoReads, mongoWrites)
}

object TradingName {

  def format: Format[String] = new Format[String] {
    override def writes(str: String): JsValue = Json.obj(
      "tradingName" -> str
    )

    override def reads(json: JsValue): JsResult[String] = json.\("tradingName").validate[String]
  }
}
