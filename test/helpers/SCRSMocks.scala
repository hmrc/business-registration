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

package helpers

import connectors.AuthConnector
import models.Authority
import models.{Metadata, MetadataResponse}
import org.mockito.ArgumentMatchers.{any, contains, eq => eqTo}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import repositories.{MetadataRepository, SequenceRepository}
import services.MetadataService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier

trait SCRSMocks {
  this: MockitoSugar =>

  lazy val mockMetadataService = mock[MetadataService]
  lazy val mockMetadataRepository = mock[MetadataRepository]
  lazy val mockAuthConnector = mock[AuthConnector]
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

  object AuthenticationMocks {
    def getCurrentAuthority(authority: Option[Authority]): OngoingStubbing[Future[Option[Authority]]] = {
      when(mockAuthConnector.getCurrentAuthority()(any[HeaderCarrier]()))
        .thenReturn(Future.successful(authority))
    }
  }

  object AuthorisationMock {
    def mockSuccessfulAuthorisation(registrationId: String, authority: Authority) = {
      when(mockMetadataRepository.getInternalId(eqTo(registrationId))).
        thenReturn(Future.successful(Some((registrationId, authority.ids.internalId))))
      when(mockAuthConnector.getCurrentAuthority()(any()))
        .thenReturn(Future.successful(Some(authority)))
    }

    def mockNotLoggedIn = {
      when(mockAuthConnector.getCurrentAuthority()(any[HeaderCarrier]()))
        .thenReturn(Future.successful(None))
      when(mockMetadataRepository.getInternalId(any())).
        thenReturn(Future.successful(None))
    }

    def mockNotAuthorised(registrationId: String, authority: Authority) = {
      when(mockAuthConnector.getCurrentAuthority()(any()))
        .thenReturn(Future.successful(Some(authority)))
      when(mockMetadataRepository.getInternalId(eqTo(registrationId))).
        thenReturn(Future.successful(Some((registrationId, authority.ids.internalId + "xxx"))))
    }

    def mockAuthResourceNotFound(authority: Authority) = {
      when(mockAuthConnector.getCurrentAuthority()(any()))
        .thenReturn(Future.successful(Some(authority)))
      when(mockMetadataRepository.getInternalId(any())).
        thenReturn(Future.successful(None))
    }
  }
}
