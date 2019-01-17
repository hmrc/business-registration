/*
 * Copyright 2019 HM Revenue & Customs
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

import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, _}

case class Metadata(internalId: String,
                    registrationID: String,
                    formCreationTimestamp: String,
                    language: String,
                    submissionResponseEmail: Option[String],
                    completionCapacity: Option[String],
                    declareAccurateAndComplete: Boolean,
                    lastSignedIn: DateTime = Metadata.now)

object Metadata extends MetadataValidator {

  def now = DateTime.now(DateTimeZone.UTC)

  val writes: OWrites[Metadata] = (
    (__ \ "internalId").write[String] and
      (__ \ "registrationID").write[String] and
      (__ \ "formCreationTimestamp").write[String] and
      (__ \ "language").write[String](languageValidator) and
      (__ \ "submissionResponseEmail").writeNullable[String] and
      (__ \ "completionCapacity").writeNullable[String](completionCapacityValidator) and
      (__ \ "declareAccurateAndComplete").write[Boolean] and
      (__ \ "lastSignedIn").write[DateTime]
    )(unlift(Metadata.unapply))

  val reads: Reads[Metadata] = (
    (__ \ "internalId").read[String] and
      (__ \ "registrationID").read[String] and
      (__ \ "formCreationTimestamp").read[String] and
      (__ \ "language").read[String](languageValidator) and
      (__ \ "submissionResponseEmail").readNullable[String] and
      (__ \ "completionCapacity").readNullable[String](completionCapacityValidator) and
      (__ \ "declareAccurateAndComplete").read[Boolean] and
      (__ \ "lastSignedIn").read[DateTime].orElse(Reads.pure(Metadata.now))
    )(Metadata.apply _)

  implicit val formats = OFormat(reads, writes)
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
