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

package repositories

import javax.inject.{Inject, Singleton}

import auth.AuthorisationResource
import com.google.inject.ImplementedBy
import models.{Metadata, MetadataResponse}
import org.joda.time.DateTime
import play.api.{Application, Logger}
import play.api.libs.json.Format
import reactivemongo.api.DB
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson._
import uk.gov.hmrc.mongo.{ReactiveRepository, Repository}
import CollectionsNames.METADATA
import InjectDB.injectDB

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

@ImplementedBy(classOf[MetadataRepositoryImpl])
trait MetadataRepository extends Repository[Metadata, BSONObjectID] with AuthorisationResource[String]{
  def createMetadata(metadata: Metadata): Future[Metadata]
  def searchMetadata(internalID: String): Future[Option[Metadata]]
  def retrieveMetadata(regI: String): Future[Option[Metadata]]
  def updateMetaData(regID : String, newMetaData : MetadataResponse) : Future[MetadataResponse]
  def removeMetadata(registrationId: String): Future[Boolean]
  def updateLastSignedIn(registrationId: String, dateTime: DateTime): Future[DateTime]
}

abstract class MetadataRepositoryBase(mongo: () => DB)(implicit formats: Format[Metadata], manifest: Manifest[Metadata])
  extends ReactiveRepository[Metadata, BSONObjectID](METADATA, mongo, formats)
    with MetadataRepository

@Singleton
class MetadataRepositoryImpl @Inject()(implicit app: Application) extends MetadataRepositoryBase(injectDB(app)) {

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] = Future.sequence(
    Seq(collection.indexesManager.ensure(Index(Seq("internalId" -> IndexType.Ascending), name = Some("internalIdIndex"), unique = true)),
      collection.indexesManager.ensure(Index(Seq("registrationID" -> IndexType.Ascending), name = Some("regIDIndex"), unique = true))))

  private def internalIDMetadataSelector(internalID: String): BSONDocument = BSONDocument(
    "internalId" -> BSONString(internalID)
  )

  private def regIDMetadataSelector(registrationID: String): BSONDocument = BSONDocument(
    "registrationID" -> BSONString(registrationID)
  )

  override def updateLastSignedIn(registrationId: String, dateTime: DateTime): Future[DateTime] = {
    val selector = regIDMetadataSelector(registrationId)
    val update = BSONDocument("$set" -> BSONDocument("lastSignedIn" -> dateTime.getMillis))
    collection.update(selector, update).map(_ => dateTime)
  }

  override def createMetadata(metadata: Metadata): Future[Metadata] = {
    collection.insert(metadata).map { res =>
      metadata
    }
  }

  override def retrieveMetadata(registrationID: String): Future[Option[Metadata]] = {
    val selector = regIDMetadataSelector(registrationID)
    collection.find(selector).one[Metadata]
  }

  override def updateMetaData(regID: String, newMetaData: MetadataResponse): Future[MetadataResponse] = {
    val selector = regIDMetadataSelector(regID)
    collection.update(selector, BSONDocument("$set" -> BSONDocument("completionCapacity" -> newMetaData.completionCapacity))) map { res =>
      if (res.hasErrors) {
        Logger.error(s"Failed to update metadata. Error: ${res.errmsg.getOrElse("")} for registration ud ${newMetaData.registrationID}")
      }
      newMetaData
    }
  }

  def getInternalId(id: String): Future[Option[(String, String)]] = {
    // TODO : this can be made more efficient by performing an index scan rather than document lookup
    retrieveMetadata(id) map {
      case None => None
      case Some(m) => Some((m.registrationID, m.internalId))
    }
  }

  override def searchMetadata(internalID: String): Future[Option[Metadata]] = {
    val selector = internalIDMetadataSelector(internalID)
    collection.find(selector).one[Metadata]
  }

  override def removeMetadata(registrationId: String): Future[Boolean] = {
    val selector = regIDMetadataSelector(registrationId)
    collection.remove(selector, firstMatchOnly = true).map(_.ok)
  }
}
