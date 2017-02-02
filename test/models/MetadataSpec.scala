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

package models

import fixtures.MetadataFixture
import org.joda.time.{DateTime, DateTimeZone, Chronology}
import org.joda.time.chrono.ISOChronology
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}
import uk.gov.hmrc.play.test.UnitSpec

class MetadataSpec extends UnitSpec with JsonFormatValidation with MetadataFixture {

  def now = DateTime.now(DateTimeZone.UTC)

  "Metadata" should {

    "Be able to be parsed from JSON without a last signed in entry " in {
      val json = Json.parse(
        """
          |{
          | "internalId":"tiid",
          | "registrationID":"regId",
          | "formCreationTimestamp":"2001-12-31T12:00:00Z",
          | "language":"ENG",
          | "submissionResponseEmail":"email@test.com",
          | "completionCapacity":"Director",
          | "declareAccurateAndComplete":true
          |}""".stripMargin)

      val result = json.validate[Metadata]
      result.isSuccess shouldBe true
      val expected = Metadata("tiid", "regId", "2001-12-31T12:00:00Z", "ENG", Some("email@test.com"), Some("Director"), true, result.get.lastSignedIn)

      result.get shouldBe expected
    }

    "Be able to be parsed from JSON and the generated date time is now " in {
      val json = Json.parse(
        """
          |{
          | "internalId":"tiid",
          | "registrationID":"regId",
          | "formCreationTimestamp":"2001-12-31T12:00:00Z",
          | "language":"ENG",
          | "submissionResponseEmail":"email@test.com",
          | "completionCapacity":"Director",
          | "declareAccurateAndComplete":true
          |}""".stripMargin)

      val before = now.getMillis
      val lastSignedIn = Json.fromJson[Metadata](json)(Metadata.reads).get.lastSignedIn.getMillis
      val after = now.getMillis

      lastSignedIn >= before && lastSignedIn <= after shouldBe true
    }

    "Be able to be parsed from a full JSON structure " in {
      val dateTime = DateTime.now(ISOChronology.getInstance())
      val json = Json.parse(
        s"""
          |{
          | "internalId":"tiid",
          | "registrationID":"regId",
          | "formCreationTimestamp":"2001-12-31T12:00:00Z",
          | "language":"ENG",
          | "submissionResponseEmail":"email@test.com",
          | "completionCapacity":"Director",
          | "declareAccurateAndComplete":true,
          | "lastSignedIn" : ${dateTime.getMillis}
          |}""".stripMargin)

      val result = json.validate[Metadata]
      result.isSuccess shouldBe true
      val expected = Metadata("tiid", "regId", "2001-12-31T12:00:00Z", "ENG", Some("email@test.com"), Some("Director"), true, dateTime)

      result.get shouldBe expected
    }

    "fail validation when an invalid completion capacity is present" in {
      val json = Json.parse(
        """
          |{
          | "internalId":"tiid",
          | "registrationID":"regId",
          | "formCreationTimestamp":"2001-12-31T12:00:00Z",
          | "language":"ENG",
          | "submissionResponseEmail":"email@test.com",
          | "completionCapacity":"1234£$%test-to-fail",
          | "declareAccurateAndComplete":true
          |}""".stripMargin)
      val result = json.validate[Metadata]

      //shouldHaveErrors(result, JsPath \ "completionCapacity", Seq(ValidationError("Invalid completion capacity")))
    }

    "fail validation when an invalid language is present" in {
      val json = Json.parse(
        """
          |{
          | "internalId":"tiid",
          | "registrationID":"regId",
          | "formCreationTimestamp":"2001-12-31T12:00:00Z",
          | "language":"test-to-fail",
          | "submissionResponseEmail":"email@test.com",
          | "completionCapacity":"Director",
          | "declareAccurateAndComplete":true
          |}""".stripMargin)
      val result = json.validate[Metadata]

      //shouldHaveErrors(result, JsPath \ "language", Seq(ValidationError("Language must either be 'ENG' or 'CYM'")))
    }
  }

  "MetadataResponse" should {

    "Be able to be parsed from JSON" in {
      val json = Json.parse("""{"registrationID":"0123456789","formCreationTimestamp":"2001-12-31T12:00:00Z","language":"ENG","completionCapacity":"Director"}""")
      val expected = buildMetadataResponse()
      val result = json.validate[MetadataResponse]

      shouldBeSuccess(expected, result)
    }

    "fail validation when an incorrect language is present" in {
      val json = Json.parse("""{"registrationID":"0123456789","formCreationTimestamp":"2001-12-31T12:00:00Z","language":"test-to-fail","completionCapacity":"Director"}""")
      val result = json.validate[MetadataResponse]

      shouldHaveErrors(result, JsPath \ "language", Seq(ValidationError("Language must either be 'ENG' or 'CYM'")))
    }

    "fail validation when an incorrect completion capacity is present" in {
      val json = Json.parse("""{"registrationID":"0123456789","formCreationTimestamp":"2001-12-31T12:00:00Z","language":"ENG","completionCapacity":"123£$test-to-fail"}""")
      val result = json.validate[MetadataResponse]

      shouldHaveErrors(result, JsPath \ "completionCapacity", Seq(ValidationError("Invalid completion capacity")))
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
