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

package services

import akka.event.slf4j.Logger
import models.{Links, Metadata, MetadataResponse}
import play.api.libs.json.{JsObject, Json}
import repositories._

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class MetadataService @Inject()(metadataRepository: MetadataMongoRepository, sequenceRepository: SequenceMongoRepository) {

  def createMetadataRecord(internalID: String, lang: String)(implicit ec: ExecutionContext): Future[MetadataResponse] = {
    generateRegistrationId flatMap { regID =>
      val newMetadata = Metadata(
        internalID,
        regID.toString,
        Instant.now().toString,
        lang,
        None,
        None,
        declareAccurateAndComplete = false
      )
      metadataRepository.createMetadata(newMetadata).map(meta => MetadataResponse.toMetadataResponse(meta))

    }
  }

  private def generateRegistrationId(implicit ec: ExecutionContext): Future[Int] = {
    sequenceRepository.getNext("registrationID")
  }

  def searchMetadataRecord(internalID: String)(implicit ec: ExecutionContext): Future[Option[MetadataResponse]] = {
    metadataRepository.searchMetadata(internalID).map {
      case Some(data) => Some(MetadataResponse.toMetadataResponse(data))
      case None => None
    }
  }

  def retrieveMetadataRecord(registrationID: String)(implicit ec: ExecutionContext): Future[Option[MetadataResponse]] = {
    metadataRepository.retrieveMetadata(registrationID).map {
      case Some(data) => Some(MetadataResponse.toMetadataResponse(data))
      case None => None
    }
  }

  def updateMetaDataRecord(registrationID: String, newMetaData: MetadataResponse)(implicit ec: ExecutionContext): Future[MetadataResponse] = {
    metadataRepository.updateMetaData(registrationID, newMetaData)
  }

  def removeMetadata(registrationId: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    metadataRepository.removeMetadata(registrationId)
  }

  def updateLastSignedIn(registrationId: String, instant: Instant)(implicit ec: ExecutionContext): Future[Instant] = {
    metadataRepository.updateLastSignedIn(registrationId, instant)
  }

  def buildSelfLink(registrationId: String): JsObject = {
    Json.obj("links" -> Links(Some(controllers.routes.MetadataController.retrieveMetadata(registrationId).url)))
  }

  def checkCompletionCapacity(regIds: Seq[String])(implicit ec: ExecutionContext): Future[Seq[Boolean]] = {
    val logger = "StartUp"

    def check(regId: String) = {
      metadataRepository.retrieveMetadata(regId) map {
        case Some(doc) =>
          val cc = doc.completionCapacity.getOrElse("[Not found]")
          Logger(logger).warn(s"Current completion capacity for regid: $regId is $cc")
          true
        case _ =>
          if (regId.nonEmpty) Logger(logger).warn(s"No document found for regId: $regId")
          false
      } recover {
        case e =>
          Logger(logger).error(s"Data check was unsuccessful for regId: $regId with error ${e.getMessage}")
          false
      }
    }

    Future.sequence(regIds.map(check))
  }

  def updateCompletionCapacity(regId: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    metadataRepository.updateCompletionCapacity(regId, "director") map {
      result =>
        Logger("StartUpUpdateCompletionCapacity").info(s"updated completion capacity for regId: $regId - $result")
        true
    }
  }
}
