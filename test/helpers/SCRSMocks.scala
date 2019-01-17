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

package helpers

import models.{Metadata, MetadataResponse}
import org.mockito.ArgumentMatchers.{any, contains, eq => eqTo}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import repositories.{MetadataRepository, SequenceRepository}
import services.MetadataService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait SCRSMocks {
  this: MockitoSugar =>

  lazy val mockMetadataService = mock[MetadataService]
  lazy val mockMetadataRepository = mock[MetadataRepository]
  lazy val mockSequenceRepository = mock[SequenceRepository]

  def matchExact(toMatch: String) = s"""\b$toMatch\b"""

  object SequenceRepositoryMocks {
    def getNext(sequence: String, returns: Int) = {
      when(mockSequenceRepository.getNext(contains(sequence)))
        .thenReturn(Future.successful(returns))
    }
  }

  object MetadataServiceMocks {
    def createMetadataRecord(result: MetadataResponse): OngoingStubbing[Future[MetadataResponse]] = {
      when(mockMetadataService.createMetadataRecord(any(), any()))
        .thenReturn(Future.successful(result))
    }

    def searchMetadataRecord(internalId: String, result: Option[MetadataResponse]): OngoingStubbing[Future[Option[MetadataResponse]]] = {
      when(mockMetadataService.searchMetadataRecord(any()))
        .thenReturn(Future.successful(result))
    }

    def retrieveMetadataRecord(regId: String, result: Option[MetadataResponse]  ): OngoingStubbing[Future[Option[MetadataResponse]]] = {
      when(mockMetadataService.retrieveMetadataRecord(eqTo(regId)))
        .thenReturn(Future.successful(result))
    }

    def removeMetadataRecord(regId: String, result: Boolean): OngoingStubbing[Future[Boolean]] = {
      when(mockMetadataService.removeMetadata(eqTo(regId)))
        .thenReturn(Future.successful(result))
    }
  }

  object MetadataRepositoryMocks {
    def createMetadata(metadata: Metadata): OngoingStubbing[Future[Metadata]] = {
      when(mockMetadataRepository.createMetadata(any[Metadata]()))
        .thenReturn(Future.successful(metadata))
    }

    def searchMetadata(internalID: String, metadata: Option[Metadata]): OngoingStubbing[Future[Option[Metadata]]] = {
      when(mockMetadataRepository.searchMetadata(any()))
        .thenReturn(Future.successful(metadata))
    }

    def retrieveMetadata(regID: String, metadata: Option[Metadata]): OngoingStubbing[Future[Option[Metadata]]] = {
      when(mockMetadataRepository.retrieveMetadata(any()))
        .thenReturn(Future.successful(metadata))
    }
  }

}
