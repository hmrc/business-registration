/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.libs.json.JsObject
import play.api.{Configuration, Logger}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.Index
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json._
import repositories.CollectionsNames
import repositories.CollectionsNames.CONTACTDETAILS
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.collection.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsRepository @Inject()(mongo: ReactiveMongoComponent, val configuration: Configuration) extends
  ReactiveRepository[ContactDetails, BSONObjectID](
    collectionName = CONTACTDETAILS,
    mongo.mongoConnector.db,
    ContactDetails.formats
  ) with TTLIndexing[ContactDetails, BSONObjectID] {

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] = {
    for {
      ttlIndexes <- ensureTTLIndexes
      indexes <- super.ensureIndexes
      _ <- fetchLatestIndexes
    } yield {
      indexes ++ ttlIndexes
    }
  }

  private def fetchLatestIndexes: Future[List[Index]] = {
    collection.indexesManager.list() map { indexes =>
      indexes.map { index =>
        val indexOptions = index.options.elements.toString()
        Logger.info(s"[EnsuringIndexes] Collection : ${CollectionsNames.CONTACTDETAILS} \n" +
          s"Index : ${index.eventualName} \n" +
          s"""keys : ${
            index.key match {
              case Seq(s@_*) => s"$s\n"
              case Nil => "None\n"
            }
          }""" +
          s"Is Unique? : ${index.unique}\n" +
          s"In Background? : ${index.background}\n" +
          s"Is sparse? : ${index.sparse}\n" +
          s"version : ${index.version}\n" +
          s"partialFilter : ${index.partialFilter.map(_.values)}\n" +
          s"Options : $indexOptions")
        index
      }
    }
  }


  def upsertContactDetails(registrationID: String, intID: String, contactDetails: ContactDetails)(implicit ec: ExecutionContext): Future[Option[ContactDetails]] = {
    val selector = BSONDocument("_id" -> registrationID, "InternalID" -> intID)
    getContactDetailsUnVerifiedUser(registrationID, intID).flatMap { res =>
      val js = ContactDetails.mongoWrites(registrationID, internalID = intID, originalContactDetails = res).writes(contactDetails)
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
