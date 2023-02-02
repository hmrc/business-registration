/*
 * Copyright 2023 HM Revenue & Customs
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

package models.prepop

import models.prepop.ContactDetails.formats
import play.api.Logging
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import scala.util.control.NoStackTrace


case class ContactDetails(firstName: Option[String],
                          middleName: Option[String],
                          surname: Option[String],
                          email: Option[String],
                          telephoneNumber: Option[String],
                          mobileNumber: Option[String]
                         )

case class PermissionDenied(registrationID: String, internalID: String) extends NoStackTrace with Logging {
  logger.error("The contact details allocated to the RegistrationID: " + registrationID + ", was requested to be accessed by a user that appeared authorised " +
    "but their internalID: " + internalID + " did not match the records InternalID")
}


object ContactDetails {
  implicit val formats: OFormat[ContactDetails] = Json.format[ContactDetails]

  def apply(originalContactDetails: ContactDetails, newContactDetails: ContactDetails): ContactDetails =
    ContactDetails(
      newContactDetails.firstName.fold(originalContactDetails.firstName)(Some(_)),
      newContactDetails.middleName.fold(originalContactDetails.middleName)(Some(_)),
      newContactDetails.surname.fold(originalContactDetails.surname)(Some(_)),
      newContactDetails.email.fold(originalContactDetails.email)(Some(_)),
      newContactDetails.telephoneNumber.fold(originalContactDetails.telephoneNumber)(Some(_)),
      newContactDetails.mobileNumber.fold(originalContactDetails.mobileNumber)(Some(_))
    )
}

case class MongoContactDetails(registrationID: String,
                               internalID: String,
                               contactDetails: Option[ContactDetails],
                               dateTime: Instant = Instant.now())

object MongoContactDetails {

  val mongoReads: Reads[MongoContactDetails] = (
    (__ \ "_id").read[String] and
      (__ \ "InternalID").read[String] and
      (__ \ "ContactDetails").readNullable[ContactDetails] and
      (__ \ "lastUpdated").read[Instant](MongoJavatimeFormats.instantReads)
    )(MongoContactDetails.apply _)

  val mongoWrites: OWrites[MongoContactDetails] = (cd: MongoContactDetails) =>
    Json.obj(
      "_id" -> cd.registrationID,
      "InternalID" -> cd.internalID,
      "ContactDetails" -> Json.toJson(cd.contactDetails),
      "lastUpdated" -> Json.toJson(cd.dateTime)(MongoJavatimeFormats.instantWrites)
    )

  val mongoFormat: Format[MongoContactDetails] = Format(mongoReads, mongoWrites)
}
