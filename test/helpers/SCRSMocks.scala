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
import repositories.{SequenceRepository, MetadataMongoRepository}
import services.MetadataService
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait SCRSMocks {
  this: MockitoSugar =>

  lazy val mockMetadataService = mock[MetadataService]
  lazy val mockMetadataRepository = mock[MetadataMongoRepository]
  lazy val mockAuthConnector = mock[AuthConnector]
  lazy val mockSequenceRepository = mock[SequenceRepository]

  def matchExact(toMatch: String) = s"""\b$toMatch\b"""

  object SequenceRepositoryMocks {
    def getNext(sequence: String, returns: Int) = {
      when(mockSequenceRepository.getNext(Matchers.contains(sequence)))
        .thenReturn(Future.successful(returns))
    }
  }

  object MetadataServiceMocks {
    def createMetadataRecord(result: MetadataResponse): OngoingStubbing[Future[MetadataResponse]] = {
      when(mockMetadataService.createMetadataRecord(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(result))
    }

    def searchMetadataRecord(internalId: String, result: Option[MetadataResponse]): OngoingStubbing[Future[Option[MetadataResponse]]] = {
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

    def searchMetadata(internalID: String, metadata: Option[Metadata]): OngoingStubbing[Future[Option[Metadata]]] = {
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

  object AuthorisationMock {
    def mockSuccessfulAuthorisation(registrationId: String, authority: Authority) = {
      when(mockMetadataRepository.getOid(Matchers.eq(registrationId))).
        thenReturn(Future.successful(Some((registrationId, authority.ids.internalId))))
      when(mockAuthConnector.getCurrentAuthority()(Matchers.any()))
        .thenReturn(Future.successful(Some(authority)))
    }

    def mockNotLoggedIn = {
      when(mockAuthConnector.getCurrentAuthority()(Matchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(None))
      when(mockMetadataRepository.getOid(Matchers.any())).
        thenReturn(Future.successful(None))
    }

    def mockNotAuthorised(registrationId: String, authority: Authority) = {
      when(mockAuthConnector.getCurrentAuthority()(Matchers.any()))
        .thenReturn(Future.successful(Some(authority)))
      when(mockMetadataRepository.getOid(Matchers.eq(registrationId))).
        thenReturn(Future.successful(Some((registrationId, authority.ids.internalId + "xxx"))))
    }

    def mockAuthResourceNotFound(authority: Authority) = {
      when(mockAuthConnector.getCurrentAuthority()(Matchers.any()))
        .thenReturn(Future.successful(Some(authority)))
      when(mockMetadataRepository.getOid(Matchers.any())).
        thenReturn(Future.successful(None))
    }
  }
}
