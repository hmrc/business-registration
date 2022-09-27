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

package utils

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsNumber, JsString}

import java.time.Instant

class InstantJsonUtilSpec extends PlaySpec with InstantJsonUtil {

  "flexibleInstantReads" must {

    "parse a date only" in {
      flexibleInstantReads.reads(JsString("1990-09-12")).get mustBe Instant.parse("1990-09-12T00:00:00.000Z")
    }

    "parse a date and time (no millis)" in {
      flexibleInstantReads.reads(JsString("1990-09-12T15:30:12")).get mustBe Instant.parse("1990-09-12T15:30:12.000Z")
    }

    "parse a date and time (with millis (1dp))" in {
      flexibleInstantReads.reads(JsString("1990-09-12T15:30:12.9")).get mustBe Instant.parse("1990-09-12T15:30:12.900Z")
    }

    "parse a date and time (with millis (2dp))" in {
      flexibleInstantReads.reads(JsString("1990-09-12T15:30:12.95")).get mustBe Instant.parse("1990-09-12T15:30:12.950Z")
    }

    "parse a date and time (with millis (3dp))" in {
      flexibleInstantReads.reads(JsString("1990-09-12T15:30:12.956")).get mustBe Instant.parse("1990-09-12T15:30:12.956Z")
    }

    "parse a date and time (with millis (3dp)) AND Zone (GMT)" in {
      flexibleInstantReads.reads(JsString("1990-09-12T15:30:12.956GMT")).get mustBe Instant.parse("1990-09-12T15:30:12.956Z")
    }

    "parse a date and time (with millis (3dp)) AND Zone (UTC)" in {
      flexibleInstantReads.reads(JsString("1990-09-12T15:30:12.956Z")).get mustBe Instant.parse("1990-09-12T15:30:12.956Z")
    }

    "parse epochMillis" in {
      flexibleInstantReads.reads(JsNumber(1)).get mustBe Instant.parse("1970-01-01T00:00:00.001Z")
    }
  }

  "milliInstantReads" must {

    "parse epochMillis" in {
      milliInstantReads.reads(JsNumber(1)).get mustBe Instant.parse("1970-01-01T00:00:00.001Z")
    }
  }

  "milliInstantWrites" must {

    "write out to epochMillis" in {
      milliInstantWrites.writes(Instant.parse("1970-01-01T00:00:00.001Z")) mustBe JsNumber(1)
    }
  }
}
