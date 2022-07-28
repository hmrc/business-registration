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

package models

import play.api.libs.json.Reads.pattern
import play.api.libs.json._

object Validation {

  def readToFmt(rds: Reads[String])(implicit wts: Writes[String]): Format[String] = Format(rds, wts)

  def withFilter[A](fmt: Format[A], error: JsonValidationError)(f: (A) => Boolean): Format[A] = {
    Format(fmt.filter(error)(f), fmt)
  }
}

trait MetadataValidator {

  import Validation.readToFmt

  val languageValidator: Format[String] = readToFmt(pattern("""^(ENG|CYM)$""".r, "Language must either be 'ENG' or 'CYM'"))

  val completionCapacityValidator: Format[String] = readToFmt(pattern("""^[A-Za-z0-9 '\\-]{1,100}$""".r, "Invalid completion capacity"))
}
