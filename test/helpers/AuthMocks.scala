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

import auth.AuthorisationResource
import connectors.AuthConnector
import fixtures.AuthFixture
import models.{Authority, UserIds}
import org.scalatest.mockito.MockitoSugar
import repositories.MetadataRepository
import uk.gov.hmrc.play.http.HeaderCarrier
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}

import scala.annotation.implicitNotFound
import scala.concurrent.Future

@implicitNotFound("Could not find an implicit AuthConnector in scope")
trait AuthMocks extends AuthFixture {
  self: MockitoSugar =>

  def mockGetCurrentAuthority(authority: Option[Authority] = Some(Authority("x", "z", UserIds("tiid","teid"))))(implicit mockAuthConnector: AuthConnector) = {
    when(mockAuthConnector.getCurrentAuthority()(any[HeaderCarrier]()))
      .thenReturn(Future.successful(authority))
  }

  def mockSuccessfulAuthorisation(resource: AuthorisationResource[String], regId: String, authority: Authority)(implicit mockAuthConnector: AuthConnector) = {
    when(resource.getInternalId(eqTo(regId)))
      .thenReturn(Future.successful(Some((regId, authority.ids.internalId))))
    when(mockAuthConnector.getCurrentAuthority()(any()))
      .thenReturn(Future.successful(Some(authority)))
  }

  def mockSuccessfulAuthorisation(mockMetadataRepo: MetadataRepository, registrationId: String, authority: Authority)(implicit mockAuthConnector: AuthConnector) = {
    when(mockMetadataRepo.getInternalId(eqTo(registrationId)))
      .thenReturn(Future.successful(Some((registrationId, authority.ids.internalId))))
    when(mockAuthConnector.getCurrentAuthority()(any()))
      .thenReturn(Future.successful(Some(authority)))
  }

  def mockNotLoggedIn(mockMetadataRepo: MetadataRepository)(implicit mockAuthConnector: AuthConnector) = {
    when(mockAuthConnector.getCurrentAuthority()(any[HeaderCarrier]()))
      .thenReturn(Future.successful(None))
    when(mockMetadataRepo.getInternalId(any()))
      .thenReturn(Future.successful(None))
  }

  def mockNotAuthorised(mockMetadataRepo: MetadataRepository, registrationId: String, authority: Authority)(implicit mockAuthConnector: AuthConnector) = {
    when(mockAuthConnector.getCurrentAuthority()(any()))
      .thenReturn(Future.successful(Some(authority)))
    when(mockMetadataRepo.getInternalId(eqTo(registrationId)))
      .thenReturn(Future.successful(Some((registrationId, authority.ids.internalId + "xxx"))))
  }

  def mockAuthResourceNotFound(mockMetadataRepo: MetadataRepository, authority: Authority)(implicit mockAuthConnector: AuthConnector) = {
    when(mockAuthConnector.getCurrentAuthority()(any()))
      .thenReturn(Future.successful(Some(authority)))
    when(mockMetadataRepo.getInternalId(any()))
      .thenReturn(Future.successful(None))
  }
}
