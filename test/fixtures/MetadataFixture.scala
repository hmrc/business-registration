/*
 * Copyright 2016 HM Revenue & Customs
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

import models.Metadata
import play.api.libs.json.{Json, JsValue}

trait MetadataFixture {

  lazy val validMetadata = Metadata(
    OID = "0123456789",
    registrationID = "0123456789",
    formCreationTimestamp = "String",
    openIDContentId = "0123456789",
    language = "en",
    submissionResponseEmail = "test@email.co.uk",
    completionCapacity = "String",
    completionCapacityOther = "String",
    declareAccurateAndComplete = "String")

  lazy val validMetadataJson: JsValue = Json.toJson(validMetadata)

  //todo: something here is wrong - use validMetadataJson instead
  lazy val validMetadataJsValue: JsValue =
    Json.toJson("""{
                   |    "OID": "0123456789",
                   |    "registrationID": "0123456789",
                   |    "formCreationTimestamp": "test",
                   |    "openIDContentId": "0123456789",
                   |    "language": "en",
                   |    "submissionResponseEmail": "test@email.co.uk",
                   |    "completionCapacity": "test",
                   |    "completionCapacityOther": "test",
                   |    "declareAccurateAndComplete": "test"
                   |}""".stripMargin)
}
