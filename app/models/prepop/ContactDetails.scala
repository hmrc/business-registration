/*
 * Copyright 2018 HM Revenue & Customs
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


case class ContactDetails(firstName:Option[String],
                         middleName:Option[String],
                         surname:Option[String],
                         email: Option[String],
                         telephoneNumber: Option[String],
                         mobileNumber: Option[String]) {

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
        if(cd.firstName.isDefined)cd.firstName else originalContactDetails.flatMap(s => s.firstName),
        if(cd.middleName.isDefined)cd.middleName else originalContactDetails.flatMap(s => s.middleName),
        if(cd.surname.isDefined)cd.surname else originalContactDetails.flatMap(s => s.surname),
        if(cd.email.isDefined)cd.email else originalContactDetails.flatMap(s => s.email),
        if(cd.telephoneNumber.isDefined)cd.telephoneNumber else originalContactDetails.flatMap(s => s.telephoneNumber),
        if(cd.mobileNumber.isDefined)cd.mobileNumber else originalContactDetails.flatMap(s => s.mobileNumber)
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
