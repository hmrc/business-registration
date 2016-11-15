/*
 * Copyright 2016 HM Revenue & Customs
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

import models.{Metadata, MetadataResponse}
import org.joda.time.DateTime
import repositories.{SequenceRepository, MetadataRepository, Repositories}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MetadataService extends MetadataService {
  override val metadataRepository = Repositories.metadataRepository
  override val sequenceRepository = Repositories.sequenceRepository
}

trait MetadataService {

  val metadataRepository: MetadataRepository
  val sequenceRepository: SequenceRepository

  def createMetadataRecord(oid: String, lang: String) : Future[MetadataResponse] = {
    generateRegistrationId flatMap { regID =>
      val newMetadata = Metadata(
        oid,
        regID.toString,
        generateTimestamp(new DateTime()),
        lang,
        None,
        None,
        declareAccurateAndComplete = false
      )
      metadataRepository.createMetadata(newMetadata).map(_.toMetadataResponse)
    }
  }

  private def generateRegistrationId: Future[Int] = {
    sequenceRepository.getNext("registrationID")
  }

  private def generateTimestamp(timeStamp: DateTime) : String = {
    val timeStampFormat = "yyyy-MM-dd'T'HH:mm:ssXXX"
    val UTC: TimeZone = TimeZone.getTimeZone("UTC")
    val format: SimpleDateFormat = new SimpleDateFormat(timeStampFormat)
    format.setTimeZone(UTC)
    format.format(new Date(timeStamp.getMillis))
  }

  def searchMetadataRecord(oID: String): Future[Option[MetadataResponse]] = {
    metadataRepository.searchMetadata(oID).map{
      case Some(data) => Some(MetadataResponse.toMetadataResponse(data))
      case None => None
    }
  }

  def retrieveMetadataRecord(registrationID: String): Future[Option[MetadataResponse]] = {
    metadataRepository.retrieveMetadata(registrationID).map{
      case Some(data) => Some(MetadataResponse.toMetadataResponse(data))
      case None => None
    }
  }

  def updateMetaDataRecord(registrationID : String, newMetaData : MetadataResponse) : Future[MetadataResponse] = {
    metadataRepository.updateMetaData(registrationID, newMetaData)
  }
}
