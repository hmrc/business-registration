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

import play.api.libs.json.Json
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Metadata(internalId: String,
                    registrationID: String,
                    formCreationTimestamp: String,
                    language: String,
                    submissionResponseEmail: Option[String],
                    completionCapacity: Option[String],
                    declareAccurateAndComplete: Boolean)

object Metadata extends MetadataValidator {
  implicit val formats = (
    (__ \ "internalId").format[String] and
    (__ \ "registrationID").format[String] and
    (__ \ "formCreationTimestamp").format[String] and
    (__ \ "language").format[String](languageValidator) and
    (__ \ "submissionResponseEmail").formatNullable[String] and
    (__ \ "completionCapacity").formatNullable[String](completionCapacityValidator) and
    (__ \ "declareAccurateAndComplete").format[Boolean]
    )(Metadata.apply, unlift(Metadata.unapply))
}

case class MetadataRequest(language: String)

object MetadataRequest extends MetadataValidator {

  implicit val formats =
    (__ \ "language").format[String](languageValidator).inmap(lang => MetadataRequest(lang), (mR: MetadataRequest) => mR.language)
    (MetadataRequest.apply _, unlift(MetadataRequest.unapply))
}


case class Links(self: Option[String],
                 registration: Option[String] = None)

object Links {
  implicit val formats = Json.format[Links]
}

case class MetadataResponse(registrationID: String,
                            formCreationTimestamp: String,
                            language: String,
                            completionCapacity : Option[String])

object MetadataResponse extends MetadataValidator {

  implicit val formats = (
    (__ \ "registrationID").format[String] and
    (__ \ "formCreationTimestamp").format[String] and
    (__ \ "language").format[String](languageValidator) and
    (__ \ "completionCapacity").formatNullable[String](completionCapacityValidator)
    )(MetadataResponse.apply, unlift(MetadataResponse.unapply))

  def toMetadataResponse(metadata: Metadata) : MetadataResponse = {
    MetadataResponse(
      metadata.registrationID,
      metadata.formCreationTimestamp,
      metadata.language,
      metadata.completionCapacity
    )
  }
}
