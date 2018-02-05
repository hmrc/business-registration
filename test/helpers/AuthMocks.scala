/*
 * Copyright 2018 HM Revenue & Customs
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
import fixtures.AuthFixture
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.{AuthConnector, MissingBearerToken}

import scala.concurrent.Future

trait AuthMocks extends AuthFixture {
  self: MockitoSugar =>

  lazy val mockAuthConnector: AuthConnector = mock[AuthConnector]

  def mockSuccessfulAuthentication: OngoingStubbing[Future[Option[String]]] = {
    when(mockAuthConnector.authorise[Option[String]](any(),any())(any(),any()))
      .thenReturn(Future.successful(Some("internal-id")))
  }

  def mockNotLoggedIn: OngoingStubbing[Future[Option[String]]] = {
    when(mockAuthConnector.authorise[Option[String]](any(),any())(any(),any()))
      .thenReturn(Future.failed(MissingBearerToken()))
  }

  def mockSuccessfulAuthorisation(resource: AuthorisationResource, regId: String): OngoingStubbing[Future[Option[String]]] = {
    when(resource.getInternalId(eqTo(regId))(any()))
      .thenReturn(Future.successful(Some("internal-id")))
    when(mockAuthConnector.authorise[Option[String]](any(),any())(any(),any()))
      .thenReturn(Future.successful(Some("internal-id")))
  }

  def mockNotAuthorised(resource: AuthorisationResource, regId: String): OngoingStubbing[Future[Option[String]]] = {
    when(mockAuthConnector.authorise[Option[String]](any(),any())(any(),any()))
      .thenReturn(Future.successful(Some("internal-id")))
    when(resource.getInternalId(eqTo(regId))(any()))
      .thenReturn(Future.successful(Some("wrong-internal-id")))
  }

  def mockAuthResourceNotFound(resource: AuthorisationResource): OngoingStubbing[Future[Option[String]]] = {
    when(mockAuthConnector.authorise[Option[String]](any(),any())(any(),any()))
      .thenReturn(Future.successful(Some("internal-id")))
    when(resource.getInternalId(any())(any()))
      .thenReturn(Future.successful(None))
  }
}
