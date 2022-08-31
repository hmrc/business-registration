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

package repositories.prepop

import models.prepop.{ContactDetails, MongoContactDetails, PermissionDenied}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model._
import repositories.CollectionsNames.CONTACTDETAILS
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsRepository @Inject()(mongo: MongoComponent,val configuration: ServicesConfig)(implicit ec: ExecutionContext) extends
  PlayMongoRepository[MongoContactDetails](
    mongoComponent = mongo,
    collectionName = CONTACTDETAILS,
    domainFormat = MongoContactDetails.mongoFormat,
    Seq(
      IndexModel(
        ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIndex")
          .expireAfter(configuration.getInt("microservice.services.prePop.ttl"), TimeUnit.SECONDS)
      )
    ),
    replaceIndexes = configuration.getBoolean("mongodb.allowReplaceTimeToLiveIndex")
  ) {

  def upsertContactDetails(registrationID: String, intID: String, contactDetails: ContactDetails)(implicit ec: ExecutionContext): Future[Option[ContactDetails]] = {

    getContactDetailsUnVerifiedUser(registrationID, intID).flatMap { optExistingDetails =>

      val update = MongoContactDetails(
        registrationID,
        intID,
        optExistingDetails.fold(Some(contactDetails))(mcd => Some(ContactDetails(mcd, contactDetails)))
      )

      collection.findOneAndReplace(
        Filters.and(equal("_id", registrationID), equal("InternalID", intID)),
        update,
        FindOneAndReplaceOptions()
          .upsert(true)
          .returnDocument(ReturnDocument.AFTER)
      ).headOption().map(_.flatMap(_.contactDetails))
    }
  }


  def getContactDetails(registrationID: String, intID: String)(implicit ec: ExecutionContext): Future[Option[ContactDetails]] =
    getContactDetailsUnVerifiedUser(registrationID, intID)

  private[repositories] def getContactDetailsWithJustRegID(registrationID: String)(implicit ec: ExecutionContext): Future[Option[MongoContactDetails]] =
    collection.find(equal("_id", registrationID)).headOption()

  def getContactDetailsUnVerifiedUser(registrationID: String, intID: String)(implicit ec: ExecutionContext): Future[Option[ContactDetails]] =
    getContactDetailsWithJustRegID(registrationID).map {
      case Some(s) if s.internalID != intID => throw PermissionDenied(registrationID, intID)
      case cd => cd.flatMap(_.contactDetails)
    }

}
