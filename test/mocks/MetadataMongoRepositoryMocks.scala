/*
 * Copyright 2021 HM Revenue & Customs
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

package mocks

import models.Metadata
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatestplus.mockito.MockitoSugar
import repositories.MetadataMongoRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait MetadataMongoRepositoryMocks {
  this: MockitoSugar =>

  lazy val mockMetadataRepository: MetadataMongoRepository = mock[MetadataMongoRepository]

  def mockCreateMetadata(metadata: Metadata): OngoingStubbing[Future[Metadata]] = {
    when(mockMetadataRepository.createMetadata(any[Metadata]()))
      .thenReturn(Future.successful(metadata))
  }

  def mockSearchMetadata(internalID: String, metadata: Option[Metadata]): OngoingStubbing[Future[Option[Metadata]]] = {
    when(mockMetadataRepository.searchMetadata(any()))
      .thenReturn(Future.successful(metadata))
  }

  def mockRetrieveMetadata(regID: String, metadata: Option[Metadata]): OngoingStubbing[Future[Option[Metadata]]] = {
    when(mockMetadataRepository.retrieveMetadata(any()))
      .thenReturn(Future.successful(metadata))
  }

}
