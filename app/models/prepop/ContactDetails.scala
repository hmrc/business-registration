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

package models.prepop

import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.libs.json._
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.util.control.NoStackTrace


case class ContactDetails(FirstName:String,
                         MiddleName:Option[String],
                         Surname:String,
                         Email: Option[String],
                         TelephoneNumber: Option[String],
                         MobileNumber: Option[String]) {

}

case class PermissionDenied(registrationID:String, internalID: String) extends NoStackTrace {
Logger.error("The contact details allocated to the RegistrationID: " + registrationID + ", was requested to be accessed by a user that appeared authorised " +
  "but their internalID: " + internalID + " did not match the records InternalID" )
}


object ContactDetails {
  implicit val formats = Json.format[ContactDetails]

  def now = DateTime.now(DateTimeZone.UTC)

  val mongoReads = new Reads[ContactDetails] {
    def reads(json: JsValue): JsResult[ContactDetails] = {
      (json \ "ContactDetails").validate[ContactDetails](ContactDetails.formats)
    }
  }

  def mongoWrites(registrationID: String, dateTime: DateTime = now,internalID: String, originalContactDetails: Option[ContactDetails]) = new OWrites[ContactDetails] {
    override def writes(cd: ContactDetails): JsObject = {

      val newCD = ContactDetails(
        cd.FirstName,
        if(cd.MiddleName.isDefined)cd.MiddleName else originalContactDetails.flatMap(s => s.MiddleName),
        cd.Surname,
        if(cd.Email.isDefined)cd.Email else originalContactDetails.flatMap(s => s.Email),
        if(cd.TelephoneNumber.isDefined)cd.TelephoneNumber else originalContactDetails.flatMap(s => s.TelephoneNumber),
        if(cd.MobileNumber.isDefined)cd.MobileNumber else originalContactDetails.flatMap(s => s.MobileNumber)
      )
        Json.obj(
        "_id" -> registrationID,
        "InternalID" -> internalID,
      "lastUpdated" -> Json.toJson(dateTime)(ReactiveMongoFormats.dateTimeWrite),
      "ContactDetails" -> Json.toJsFieldJsValueWrapper(newCD)(formats)
        )
    }
  }
}
