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

package services

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}
import javax.inject.{Inject, Singleton}

import models.{Links, Metadata, MetadataResponse}
import org.joda.time.DateTime
import play.api.libs.json.{JsObject, Json}
import repositories._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MetadataService @Inject() (mongo: MetadataMongo, sequenceRepository: SequenceRepository) {
  val metadataRepository = mongo.repository
  def createMetadataRecord(internalID: String, lang: String)(implicit ec: ExecutionContext): Future[MetadataResponse] = {
    generateRegistrationId flatMap { regID =>
      val newMetadata = Metadata(
        internalID,
        regID.toString,
        generateTimestamp(new DateTime()),
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

  private def generateTimestamp(timeStamp: DateTime) : String = {
    val timeStampFormat = "yyyy-MM-dd'T'HH:mm:ssXXX"
    val UTC: TimeZone = TimeZone.getTimeZone("UTC")
    val format: SimpleDateFormat = new SimpleDateFormat(timeStampFormat)
    format.setTimeZone(UTC)
    format.format(new Date(timeStamp.getMillis))
  }

  def searchMetadataRecord(internalID: String)(implicit ec: ExecutionContext): Future[Option[MetadataResponse]] = {
    metadataRepository.searchMetadata(internalID).map{
      case Some(data) => Some(MetadataResponse.toMetadataResponse(data))
      case None => None
    }
  }

  def retrieveMetadataRecord(registrationID: String)(implicit ec: ExecutionContext): Future[Option[MetadataResponse]] = {
    metadataRepository.retrieveMetadata(registrationID).map{
      case Some(data) => Some(MetadataResponse.toMetadataResponse(data))
      case None => None
    }
  }

  def updateMetaDataRecord(registrationID : String, newMetaData : MetadataResponse)(implicit ec: ExecutionContext): Future[MetadataResponse] = {
    metadataRepository.updateMetaData(registrationID, newMetaData)
  }

  def removeMetadata(registrationId: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    metadataRepository.removeMetadata(registrationId)
  }

  def updateLastSignedIn(registrationId: String, dateTime: DateTime)(implicit ec: ExecutionContext): Future[DateTime] = {
    metadataRepository.updateLastSignedIn(registrationId, dateTime)
  }

  def buildSelfLink(registrationId: String): JsObject = {
    Json.obj("links" -> Links(Some(controllers.routes.MetadataController.retrieveMetadata(registrationId).url)))
  }
}
