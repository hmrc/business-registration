/*
 * Copyright 2021 HM Revenue & Customs
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

package repositories

import auth.AuthorisationResource
import javax.inject.{Inject, Singleton}
import models.{Metadata, MetadataResponse}
import org.joda.time.DateTime
import play.api.Logging
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson._
import reactivemongo.play.json._
import repositories.CollectionsNames.METADATA
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MetadataMongoRepository @Inject()(mongo: ReactiveMongoComponent) extends
  ReactiveRepository[Metadata, BSONObjectID](
    METADATA,
    mongo.mongoConnector.db,
    Metadata.formats
  ) with AuthorisationResource {

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] = Future.sequence(
    Seq(collection.indexesManager.ensure(Index(Seq("internalId" -> IndexType.Ascending), name = Some("internalIdIndex"), unique = true)),
      collection.indexesManager.ensure(Index(Seq("registrationID" -> IndexType.Ascending), name = Some("regIDIndex"), unique = true))))

  private def internalIDMetadataSelector(internalID: String): BSONDocument = BSONDocument(
    "internalId" -> BSONString(internalID)
  )

  private def regIDMetadataSelector(registrationID: String): BSONDocument = BSONDocument(
    "registrationID" -> BSONString(registrationID)
  )

  def updateCompletionCapacity(registrationId: String, completionCapacity: String)(implicit ec: ExecutionContext): Future[String] = {
    val selector = regIDMetadataSelector(registrationId)
    val update = BSONDocument("$set" -> BSONDocument("completionCapacity" -> completionCapacity))
    collection.update(selector, update) map (_ => completionCapacity)
  }

  def updateLastSignedIn(registrationId: String, dateTime: DateTime)(implicit ec: ExecutionContext): Future[DateTime] = {
    val selector = regIDMetadataSelector(registrationId)
    val update = BSONDocument("$set" -> BSONDocument("lastSignedIn" -> dateTime.getMillis))
    collection.update(selector, update).map(_ => dateTime)
  }

  def createMetadata(metadata: Metadata)(implicit ec: ExecutionContext): Future[Metadata] = {
    collection.insert(metadata).map { res =>
      metadata
    }
  }

  def retrieveMetadata(registrationID: String)(implicit ec: ExecutionContext): Future[Option[Metadata]] = {
    val selector = regIDMetadataSelector(registrationID)
    collection.find(selector).one[Metadata]
  }

  def updateMetaData(regID: String, newMetaData: MetadataResponse)(implicit ec: ExecutionContext): Future[MetadataResponse] = {
    val selector = regIDMetadataSelector(regID)
    collection.update(selector, BSONDocument("$set" -> BSONDocument("completionCapacity" -> newMetaData.completionCapacity))) map { res =>
      if (!res.ok) {
        logger.error(s"Failed to update metadata. Error: ${res.errmsg.getOrElse("")} for registration ud ${newMetaData.registrationID}")
      }
      newMetaData
    }
  }

  def getInternalId(id: String)(implicit ec: ExecutionContext): Future[Option[String]] = {
    // TODO : this can be made more efficient by performing an index scan rather than document lookup
    retrieveMetadata(id) map {
      case None => None
      case Some(m) => Some(m.internalId)
    }
  }

  def searchMetadata(internalID: String)(implicit ec: ExecutionContext): Future[Option[Metadata]] = {
    val selector = internalIDMetadataSelector(internalID)
    collection.find(selector).one[Metadata]
  }

  def removeMetadata(registrationId: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val selector = regIDMetadataSelector(registrationId)
    collection.remove(selector, firstMatchOnly = true).map(_.ok)
  }
}
