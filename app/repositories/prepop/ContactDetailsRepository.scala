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

package repositories.prepop

import javax.inject.{Inject, Singleton}

import models.prepop.{ContactDetails, PermissionDenied}
import play.api.Configuration
import play.api.libs.json.JsObject
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.DB
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json._
import repositories.CollectionsNames.CONTACTDETAILS
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsMongo  @Inject()(mongo: ReactiveMongoComponent, configuration: Configuration) {
  val repository = new ContactDetailsRepoMongo(mongo.mongoConnector.db, configuration)
}

trait ContactDetailsRepository{
  def upsertContactDetails(registrationID: String, intID: String, contactDetails: ContactDetails)(implicit ec: ExecutionContext): Future[Option[ContactDetails]]
  def getContactDetails(registrationID: String, intID: String)(implicit ec: ExecutionContext): Future[Option[ContactDetails]]
  private[repositories] def getContactDetailsWithJustRegID(registrationID: String)(implicit ec: ExecutionContext):Future[Option[JsObject]]
  def getContactDetailsUnVerifiedUser(RegistrationID:String, InternalID:String)(implicit ec: ExecutionContext):Future[Option[ContactDetails]]
}

class ContactDetailsRepoMongo(mongo: () => DB, val configuration: Configuration)
  extends ReactiveRepository[ContactDetails, BSONObjectID](collectionName = CONTACTDETAILS, mongo, ContactDetails.formats)
  with ContactDetailsRepository with TTLIndexing[ContactDetails, BSONObjectID] {

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] = {
    for{
      ttlIndexes <- ensureTTLIndexes
      indexes    <- super.ensureIndexes
    } yield {
      indexes ++ ttlIndexes
    }
  }

  def upsertContactDetails(registrationID: String, intID: String, contactDetails: ContactDetails)(implicit ec: ExecutionContext): Future[Option[ContactDetails]] = {
    val selector = BSONDocument("_id" -> registrationID, "InternalID" -> intID)
    getContactDetailsUnVerifiedUser(registrationID, intID) .flatMap{ res =>
        val js = ContactDetails.mongoWrites(registrationID, internalID = intID,originalContactDetails = res).writes(contactDetails)
        collection.findAndUpdate(selector, js, upsert = true, fetchNewObject = true) map {
          s => s.result[ContactDetails](ContactDetails.mongoReads)
        }
      }
    }



  def getContactDetails(registrationID: String, intID: String)(implicit ec: ExecutionContext): Future[Option[ContactDetails]] = {
    val selector = BSONDocument("_id" -> registrationID, "InternalID" -> intID)
    getContactDetailsUnVerifiedUser(registrationID, intID).flatMap {
      case Some(cd: ContactDetails) => Future.successful(Some(cd))
      case _ => Future.successful(None)
    }
  }

  private[repositories] def getContactDetailsWithJustRegID(registrationID: String)(implicit ec: ExecutionContext): Future[Option[JsObject]] = {
    val selector = BSONDocument("_id" -> registrationID)
    collection.find(selector).one[JsObject]

  }

  def getContactDetailsUnVerifiedUser(registrationID: String, intID: String)(implicit ec: ExecutionContext): Future[Option[ContactDetails]] = {
    getContactDetailsWithJustRegID(registrationID).map {
      case Some(s) if ((s \ "InternalID").get.as[String] != intID) =>
     throw PermissionDenied(registrationID, intID)
      case Some(s) => Some(s.as[ContactDetails](ContactDetails.mongoReads))
      case _ => None
    }
  }

}
