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

package repositories

import auth.AuthorisationResource
import javax.inject.{Inject, Singleton}
import models.{Metadata, MetadataResponse}
import org.joda.time.DateTime
import play.api.Logger
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.DB
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson._
import reactivemongo.play.json._
import repositories.CollectionsNames.METADATA
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MetadataMongo @Inject()(mongo: ReactiveMongoComponent) {
   val repository = new MetadataRepositoryMongo(mongo.mongoConnector.db)
}

trait MetadataRepository extends AuthorisationResource {
  def createMetadata(metadata: Metadata)(implicit ec: ExecutionContext): Future[Metadata]
  def searchMetadata(internalID: String)(implicit ec: ExecutionContext): Future[Option[Metadata]]
  def retrieveMetadata(regI: String)(implicit ec: ExecutionContext): Future[Option[Metadata]]
  def updateMetaData(regID : String, newMetaData : MetadataResponse)(implicit ec: ExecutionContext): Future[MetadataResponse]
  def removeMetadata(registrationId: String)(implicit ec: ExecutionContext): Future[Boolean]
  def updateLastSignedIn(registrationId: String, dateTime: DateTime)(implicit ec: ExecutionContext): Future[DateTime]
  def updateCompletionCapacity(registrationId: String, completionCapacity: String)(implicit ec: ExecutionContext): Future[String]
}

class MetadataRepositoryMongo (mongo: () => DB) extends ReactiveRepository[Metadata, BSONObjectID](METADATA, mongo, Metadata.formats)
  with MetadataRepository {

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] = Future.sequence(
    Seq(collection.indexesManager.ensure(Index(Seq("internalId" -> IndexType.Ascending), name = Some("internalIdIndex"), unique = true)),
      collection.indexesManager.ensure(Index(Seq("registrationID" -> IndexType.Ascending), name = Some("regIDIndex"), unique = true))))

  private def internalIDMetadataSelector(internalID: String): BSONDocument = BSONDocument(
    "internalId" -> BSONString(internalID)
  )

  private def regIDMetadataSelector(registrationID: String): BSONDocument = BSONDocument(
    "registrationID" -> BSONString(registrationID)
  )

  override def updateCompletionCapacity(registrationId: String, completionCapacity: String)(implicit ec: ExecutionContext): Future[String] = {
    val selector = regIDMetadataSelector(registrationId)
    val update = BSONDocument("$set" -> BSONDocument("completionCapacity" -> completionCapacity))
    collection.update(selector, update) map(_ => completionCapacity)
  }

  override def updateLastSignedIn(registrationId: String, dateTime: DateTime)(implicit ec: ExecutionContext): Future[DateTime] = {
    val selector = regIDMetadataSelector(registrationId)
    val update = BSONDocument("$set" -> BSONDocument("lastSignedIn" -> dateTime.getMillis))
    collection.update(selector, update).map(_ => dateTime)
  }

  override def createMetadata(metadata: Metadata)(implicit ec: ExecutionContext): Future[Metadata] = {
    collection.insert(metadata).map { res =>
      metadata
    }
  }

  override def retrieveMetadata(registrationID: String)(implicit ec: ExecutionContext): Future[Option[Metadata]] = {
    val selector = regIDMetadataSelector(registrationID)
    collection.find(selector).one[Metadata]
  }

  override def updateMetaData(regID: String, newMetaData: MetadataResponse)(implicit ec: ExecutionContext): Future[MetadataResponse] = {
    val selector = regIDMetadataSelector(regID)
    collection.update(selector, BSONDocument("$set" -> BSONDocument("completionCapacity" -> newMetaData.completionCapacity))) map { res =>
      if (!res.ok) {
        Logger.error(s"Failed to update metadata. Error: ${res.errmsg.getOrElse("")} for registration ud ${newMetaData.registrationID}")
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

  override def searchMetadata(internalID: String)(implicit ec: ExecutionContext): Future[Option[Metadata]] = {
    val selector = internalIDMetadataSelector(internalID)
    collection.find(selector).one[Metadata]
  }

  override def removeMetadata(registrationId: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val selector = regIDMetadataSelector(registrationId)
    collection.remove(selector, firstMatchOnly = true).map(_.ok)
  }
}
