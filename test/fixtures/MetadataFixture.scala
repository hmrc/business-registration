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

package fixtures

import models.{MetadataRequest, Links, MetadataResponse, Metadata}
import play.api.libs.json.{JsObject, Json, JsValue}

trait MetadataFixture {

  private val INTERNAL_ID = "0123456789"
  private val REG_ID = "0123456789"
  private val TIMESTAMP = "2001-12-31T12:00:00Z"
  private val LANG = "ENG"
  private val SUB_RESP_EMAIL = Some("test@email.co.uk")
  private val COMPLETION_CAPACITY = Some("Director")
  private val DECLARE = true

  def buildMetadataRequest(lang: String = LANG) = MetadataRequest(lang)

  def buildMetadata(internalId: String = INTERNAL_ID,
                    regId: String = REG_ID,
                    timeStamp: String = TIMESTAMP,
                    lang: String = LANG,
                    subRespEmail: Option[String] = SUB_RESP_EMAIL,
                    completionCapacity: Option[String] = COMPLETION_CAPACITY,
                    declare: Boolean = DECLARE) = {
    Metadata(
      internalId = internalId,
      registrationID = regId,
      formCreationTimestamp = timeStamp,
      language = lang,
      submissionResponseEmail = subRespEmail,
      completionCapacity = completionCapacity,
      declareAccurateAndComplete = declare
    )
  }

  def buildMetadataResponse(regId: String = REG_ID,
                            timeStamp: String = TIMESTAMP,
                            lang: String = LANG,
                            completionCapacity: Option[String] = COMPLETION_CAPACITY) = {
    MetadataResponse(
      registrationID = regId,
      formCreationTimestamp = timeStamp,
      language = lang,
      completionCapacity = completionCapacity
    )
  }

  def buildSelfLinkJsObj(regId: String = REG_ID): JsObject = {
    Json.obj("links" -> Links(Some(controllers.routes.MetadataController.retrieveMetadata(regId).url)))
  }

  lazy val metadataResponseJsObj = Json.toJson(buildMetadataResponse()).as[JsObject] ++ buildSelfLinkJsObj()

  lazy val validMetadataJson: JsValue = Json.toJson(buildMetadata())
}
