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

package models

import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, Json}
import uk.gov.hmrc.play.test.UnitSpec

class MetadataSpec extends UnitSpec with JsonFormatValidation {

  "Metadata" should {

    "Be able to be parsed from JSON" in {
      val expected = Metadata("oid", "regId", "2001-12-31T12:00:00Z", "ENG", Some("email@test.com"), Some("Director"), true)
      val json = Json.parse(
        """
          |{
          | "OID":"oid",
          | "registrationID":"regId",
          | "formCreationTimestamp":"2001-12-31T12:00:00Z",
          | "language":"ENG",
          | "submissionResponseEmail":"email@test.com",
          | "completionCapacity":"Director",
          | "declareAccurateAndComplete":true
          |}""".stripMargin)
      val result = json.validate[Metadata]

      shouldBeSuccess(expected, result)
    }

    "fail validation when an invalid completion capacity is present" in {
      val json = Json.parse(
        """
          |{
          | "OID":"oid",
          | "registrationID":"regId",
          | "formCreationTimestamp":"2001-12-31T12:00:00Z",
          | "language":"ENG",
          | "submissionResponseEmail":"email@test.com",
          | "completionCapacity":"1234£$%test-to-fail",
          | "declareAccurateAndComplete":true
          |}""".stripMargin)
      val result = json.validate[Metadata]

      shouldHaveErrors(result, JsPath \ "completionCapacity", Seq(ValidationError("Invalid completion capacity")))
    }

    "fail validation when an invalid language is present" in {
      val json = Json.parse(
        """
          |{
          | "OID":"oid",
          | "registrationID":"regId",
          | "formCreationTimestamp":"2001-12-31T12:00:00Z",
          | "language":"test-to-fail",
          | "submissionResponseEmail":"email@test.com",
          | "completionCapacity":"Director",
          | "declareAccurateAndComplete":true
          |}""".stripMargin)
      val result = json.validate[Metadata]

      shouldHaveErrors(result, JsPath \ "language", Seq(ValidationError("Language must either be 'ENG' or 'CYM'")))
    }
  }

  "MetadataResponse" should {

    "Be able to be parsed from JSON" in {
      //val json = Json.toJson(MetadataResponse("regId", "2001-12-31T12:00:00Z", "ENG", Some("Director")), "")
    }
  }

  "MetadataRequest" should {

    "Be able to be parsed from JSON when language is 'ENG'" in {
      val json = Json.parse("""{"language":"ENG"}""")
      val expected = MetadataRequest("ENG")
      val result = json.validate[MetadataRequest]

      shouldBeSuccess(expected, result)
    }

    "Be able to be parsed from JSON when language is 'CYM'" in {
      val json = Json.parse("""{"language":"CYM"}""")
      val expected = MetadataRequest("CYM")
      val result = json.validate[MetadataRequest]

      shouldBeSuccess(expected, result)
    }

    "fail validation is 'ENG' or 'CYM' is not present in language" in {
      val json = Json.parse("""{"language":"toFail"}""")
      val result = json.validate[MetadataRequest]

      shouldHaveErrors(result, JsPath \ "language", Seq(ValidationError("Language must either be 'ENG' or 'CYM'")))
    }
  }
}
