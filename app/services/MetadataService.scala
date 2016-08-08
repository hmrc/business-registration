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

import models.{ErrorResponse, MetadataResponse, Metadata}
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.{Ok, Created, NotFound}
import repositories.{Repositories, MetadataRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MetadataService extends MetadataService {
  override val metadataRepository = Repositories.metadataRepository
}

trait MetadataService {

  val metadataRepository: MetadataRepository

  def createMetadataRecord(metadata: Metadata): Future[Result] = {
    val newMetadata = metadata.copy(
      registrationID = generateRegistrationId,
      formCreationTimestamp = generateTimestamp(new DateTime())
    )
    metadataRepository.createMetadata(newMetadata).map(res => Created(Json.toJson(res)))
  }

  private def generateRegistrationId: String = {
    //todo: (SCRS-2890) random number gen until we know how to create one
    scala.util.Random.nextInt("99999".toInt).toString
  }

  private def generateTimestamp(timeStamp: DateTime) : String = {
    val timeStampFormat = "yyyy-MM-dd'T'HH:mm:ssXXX"
    val UTC: TimeZone = TimeZone.getTimeZone("UTC")
    val format: SimpleDateFormat = new SimpleDateFormat(timeStampFormat)
    format.setTimeZone(UTC)
    format.format(new Date(timeStamp.getMillis))
  }

  def searchMetadataRecord(oID: String): Future[Result] = {
    metadataRepository.searchMetadata(oID).map{
      case Some(data) => Ok(Json.toJson(MetadataResponse.toMetadataResponse(data)))
      case _ => NotFound(ErrorResponse.MetadataNotFound)
    }
  }

  def retrieveMetadataRecord(registrationID: String): Future[Result] = {
    metadataRepository.retrieveMetadata(registrationID).map{
      case Some(data) => Ok(Json.toJson(MetadataResponse.toMetadataResponse(data)))
      case _ => NotFound(ErrorResponse.MetadataNotFound)
    }
  }
}