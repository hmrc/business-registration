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

package mocks

import models.MetadataResponse
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatestplus.mockito.MockitoSugar
import services.MetadataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait MetadataServiceMocks {
  this: MockitoSugar =>

  lazy val mockMetadataService: MetadataService = mock[MetadataService]

  def mockCreateMetadataRecord(result: MetadataResponse): OngoingStubbing[Future[MetadataResponse]] = {
    when(mockMetadataService.createMetadataRecord(any(), any()))
      .thenReturn(Future.successful(result))
  }

  def mockSearchMetadataRecord(internalId: String)(result: Option[MetadataResponse]): OngoingStubbing[Future[Option[MetadataResponse]]] = {
    when(mockMetadataService.searchMetadataRecord(any()))
      .thenReturn(Future.successful(result))
  }

  def mockRetrieveMetadataRecord(regId: String)(result: Option[MetadataResponse]): OngoingStubbing[Future[Option[MetadataResponse]]] = {
    when(mockMetadataService.retrieveMetadataRecord(eqTo(regId)))
      .thenReturn(Future.successful(result))
  }

  def mockRemoveMetadataRecord(regId: String)(result: Boolean): OngoingStubbing[Future[Boolean]] = {
    when(mockMetadataService.removeMetadata(eqTo(regId)))
      .thenReturn(Future.successful(result))
  }
}
