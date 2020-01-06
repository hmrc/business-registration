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

package fixtures

import models.{Metadata, MetadataResponse}
import play.api.libs.json.{JsValue, Json}

trait MetadataFixtures {

  val testInternalId: String
  val testRegistrationId: String

  def buildMetadataJson(internalId: String = testInternalId): JsValue = Json.parse(
    s"""
       |{
       | "internalId":"$internalId",
       | "registrationID":"regId",
       | "formCreationTimestamp":"2001-12-31T12:00:00Z",
       | "language":"ENG",
       | "submissionResponseEmail":"email@test.com",
       | "completionCapacity":"Director",
       | "declareAccurateAndComplete":true
       |}""".stripMargin)

  def buildMetadata(internalId: String = testInternalId, regId: String = testRegistrationId): Metadata = Metadata(
    internalId = internalId,
    registrationID = regId,
    formCreationTimestamp = "2001-12-31T12:00:00Z",
    language = "ENG",
    submissionResponseEmail = Some("email@test.com"),
    completionCapacity = Some("Director"),
    declareAccurateAndComplete = true
  )

  def buildMetadataResponse(regId: String = testRegistrationId) = {
    MetadataResponse(
      registrationID = regId,
      formCreationTimestamp = "2001-12-31T12:00:00Z",
      language = "ENG",
      completionCapacity = Some("Director")
    )
  }

  def buildMetadataResponseJson(regId: String = testRegistrationId, lang: String = "ENG", cc: String = "Director") =
    Json.parse(s"""{
                  |"registrationID": "$regId",
                  |"formCreationTimestamp": "2001-12-31T12:00:00Z",
                  |"language": "$lang",
                  |"completionCapacity": "$cc"
                  |}""".stripMargin)

  val invalidUpdateJson =
    Json.parse(s"""{
                  |"completionCapacity": "Fake"
                  |}""".stripMargin)
}
