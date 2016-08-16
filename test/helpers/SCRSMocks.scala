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

package helpers

import connectors.{AuthConnector, Authority}
import models.{Metadata, MetadataResponse}
import org.mockito.Matchers
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import play.api.libs.json.JsValue
import play.api.mvc.Result
import repositories.MetadataMongoRepository
import services.MetadataService
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait SCRSMocks {
  this: MockitoSugar =>

  lazy val mockMetadataService = mock[MetadataService]
  lazy val mockMetadataRepository = mock[MetadataMongoRepository]
  lazy val mockAuthConnector = mock[AuthConnector]

  object MetadataServiceMocks {
    def createMetadataRecord(result: MetadataResponse): OngoingStubbing[Future[MetadataResponse]] = {
      when(mockMetadataService.createMetadataRecord(Matchers.any[Metadata]()))
        .thenReturn(Future.successful(result))
    }

    def searchMetadataRecord(oid: String, result: Option[MetadataResponse]): OngoingStubbing[Future[Option[MetadataResponse]]] = {
      when(mockMetadataService.searchMetadataRecord(Matchers.any()))
        .thenReturn(Future.successful(result))
    }

    def retrieveMetadataRecord(regId: String, result: Option[MetadataResponse]  ): OngoingStubbing[Future[Option[MetadataResponse]]] = {
      when(mockMetadataService.retrieveMetadataRecord(Matchers.eq(regId)))
        .thenReturn(Future.successful(result))
    }
  }

  object MetadataRepositoryMocks {
    def createMetadata(metadata: Metadata): OngoingStubbing[Future[Metadata]] = {
      when(mockMetadataRepository.createMetadata(Matchers.any[Metadata]()))
        .thenReturn(Future.successful(metadata))
    }

    def searchMetadata(oID: String, metadata: Option[Metadata]): OngoingStubbing[Future[Option[Metadata]]] = {
      when(mockMetadataRepository.searchMetadata(Matchers.any()))
        .thenReturn(Future.successful(metadata))
    }

    def retrieveMetadata(regID: String, metadata: Option[Metadata]): OngoingStubbing[Future[Option[Metadata]]] = {
      when(mockMetadataRepository.retrieveMetadata(Matchers.any()))
        .thenReturn(Future.successful(metadata))
    }
  }

  object AuthenticationMocks {
    def getCurrentAuthority(authority: Option[Authority]): OngoingStubbing[Future[Option[Authority]]] = {
      when(mockAuthConnector.getCurrentAuthority()(Matchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(authority))
    }
  }
}
