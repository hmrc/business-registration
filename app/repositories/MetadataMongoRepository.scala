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

package repositories

import auth.AuthorisationResource
import models.{Metadata, MetadataResponse}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import play.api.Logger
import repositories.CollectionsNames.METADATA
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MetadataMongoRepository @Inject()(mongo: MongoComponent)(implicit ec: ExecutionContext) extends
  PlayMongoRepository[Metadata] (
    mongo,
    METADATA,
    Metadata.formats,
    Seq(
      IndexModel(
        ascending("internalId"),
        IndexOptions()
          .name("internalIdIndex")
          .unique(true)
      ),
      IndexModel(
        ascending("registrationID"),
        IndexOptions()
          .name("regIDIndex")
          .unique(true)
      )
    )
  ) with AuthorisationResource {

  val logger = Logger(getClass.getSimpleName)

  private def internalIDMetadataSelector(internalID: String): Bson = equal("internalId", internalID)

  private def regIDMetadataSelector(registrationID: String): Bson = equal("registrationID", registrationID)

  def updateCompletionCapacity(registrationId: String, completionCapacity: String)(implicit ec: ExecutionContext): Future[String] = {
    val selector = regIDMetadataSelector(registrationId)
    val update = set("completionCapacity", completionCapacity)
    collection.updateOne(selector, update).toFuture() map (_ => completionCapacity)
  }

  def updateLastSignedIn(registrationId: String, instant: Instant)(implicit ec: ExecutionContext): Future[Instant] = {
    val selector = regIDMetadataSelector(registrationId)
    val update = set("lastSignedIn", instant.toEpochMilli)
    collection.updateOne(selector, update).toFuture().map(_ => instant)
  }

  def createMetadata(metadata: Metadata)(implicit ec: ExecutionContext): Future[Metadata] =
    collection.insertOne(metadata).toFuture().map { _ => metadata }

  def retrieveMetadata(registrationID: String)(implicit ec: ExecutionContext): Future[Option[Metadata]] =
    collection.find(regIDMetadataSelector(registrationID)).headOption()

  def updateMetaData(regID: String, newMetaData: MetadataResponse)(implicit ec: ExecutionContext): Future[MetadataResponse] = {
    newMetaData.completionCapacity match {
      case Some(capacity) => updateCompletionCapacity(regID, capacity).map(_ => newMetaData) recover {
        case ex: Exception =>
          logger.error(s"Failed to update metadata. Error: ${ex.getMessage} for registration ud ${newMetaData.registrationID}")
          newMetaData
      }
      case None => Future.successful(newMetaData)
    }
  }

  def getInternalId(id: String)(implicit ec: ExecutionContext): Future[Option[String]] =
    retrieveMetadata(id) map {
      case None => None
      case Some(m) => Some(m.internalId)
    }

  def searchMetadata(internalID: String)(implicit ec: ExecutionContext): Future[Option[Metadata]] =
    collection.find(internalIDMetadataSelector(internalID)).headOption()

  def removeMetadata(registrationId: String)(implicit ec: ExecutionContext): Future[Boolean] =
    collection.deleteOne(regIDMetadataSelector(registrationId)).toFuture().map(_.wasAcknowledged())
}
