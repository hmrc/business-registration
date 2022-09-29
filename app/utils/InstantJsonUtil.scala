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

import play.api.libs.json.{JsNumber, Reads, Writes}

import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.time.{Instant, LocalDateTime, ZoneOffset}

trait InstantJsonUtil {

  val milliInstantWrites = Writes[Instant] {
    instant => JsNumber(instant.toEpochMilli)
  }

  val milliInstantReads = Reads[Instant] {
    _.validate[Long].map(Instant.ofEpochMilli)
  }

  private val flexibleDateFormatter = new DateTimeFormatterBuilder()
    .appendPattern("uuuu-MM-dd['T'][HH:mm:ss]")
    .optionalStart()
    .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
    .optionalEnd()
    .optionalStart()
    .appendZoneId()
    .optionalEnd()
    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
    .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
    .toFormatter

  val flexibleInstantReads = Reads[Instant] {
    case n: JsNumber => milliInstantReads.reads(n)
    case s => s.validate[String].map(date => LocalDateTime.parse(date, flexibleDateFormatter).toInstant(ZoneOffset.UTC))
  }
}
