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
import play.api.libs.json._
import Reads.{maxLength, minLength, pattern}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.functional.syntax._

object Validation {

  def readToFmt(rds: Reads[String])(implicit wts: Writes[String]): Format[String] = Format(rds, wts)

  def withFilter[A](fmt: Format[A], error: ValidationError)(f: (A) => Boolean): Format[A] = {
    Format(fmt.filter(error)(f), fmt)
  }
}

trait MetadataValidator {

  import Validation.readToFmt

  val languageValidator = readToFmt(pattern("""^(cy|CY|en|EN)$""".r))
}
