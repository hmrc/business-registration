/*
 * Copyright 2020 HM Revenue & Customs
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

package auth

import helpers.SCRSSpec
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.auth.core.{AuthConnector, MissingBearerToken}

import scala.concurrent.Future

class AuthorisationSpec extends SCRSSpec {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockResource: AuthorisationResource = mock[AuthorisationResource]

  object Authorisation extends Authorisation {
    val authConnector: AuthConnector = mockAuthConnector
    val resourceConn: AuthorisationResource = mockResource
  }

  def resultHandler(id: String, authResult: AuthorisationResult): Future[Result] = authResult match {
    case AuthResourceNotFound(_) => Future.successful(Results.NotFound)
    case _ => Future.successful(Results.Forbidden)
  }

  "The authorisation helper" should {
    "indicate there's no logged in user where there isn't a valid bearer token" in {

      when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any())).thenReturn(Future.failed(MissingBearerToken()))

      when(mockResource.getInternalId(any())(any())).thenReturn(Future.successful(None))

      val result = Authorisation.isAuthorised("xxx")(
        failure = resultHandler,
        success = {
          Future.successful(Results.Ok)
        })

      val response = await(result)

      response.header.status mustBe FORBIDDEN
    }

    "provided an authorised result when logged in and a consistent resource" in {

      val regId = "xxx"
      val intId = "internal-id"

      when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any())).thenReturn(Future.successful(Some(intId)))
      when(mockResource.getInternalId(eqTo(regId))(any())).thenReturn(Future.successful(Some(intId)))

      val result = Authorisation.isAuthorised("xxx")(
        failure = resultHandler,
        success = {
          Future.successful(Results.Ok)
        })

      val response = await(result)

      response.header.status mustBe OK
    }

    "provided a not-authorised result when logged in and an inconsistent resource" in {

      val regId = "xxx"
      val intId = "internal-id"

      when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any())).thenReturn(Future.successful(Some(intId)))
      when(mockResource.getInternalId(eqTo(regId))(any())).thenReturn(Future.successful(Some(intId + "xxx")))

      val result = Authorisation.isAuthorised("xxx")(
        failure = resultHandler,
        success = {
          Future.successful(Results.Ok)
        })

      val response = await(result)

      response.header.status mustBe FORBIDDEN
    }

    "provided a not-found result when logged in and no resource for the identifier" in {

      val intId = "internal-id"

      when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any())).thenReturn(Future.successful(Some(intId)))
      when(mockResource.getInternalId(any())(any())).thenReturn(Future.successful(None))

      val result = Authorisation.isAuthorised("xxx")(
        failure = resultHandler,
        success = {
          Future.successful(Results.Ok)
        })

      val response = await(result)

      response.header.status mustBe NOT_FOUND
    }
  }

  "mapToAuthResult" should {
    "return Authorised when context int id and resource int id match" in {
      val intId = Some("internal-id")
      val resource = Some("internal-id")
      Authorisation.mapToAuthResult(intId, resource) mustBe Authorised(intId.get)
    }

    "return NotLoggedInOrAuthorised when context is not passed in" in {
      val resource = Some("foo1")
      Authorisation.mapToAuthResult(None, resource) mustBe NotLoggedInOrAuthorised
    }

    "return AuthResourceNotFound when context is passed in, but resource is not passed in" in {
      val intId = Some("internal-id")
      Authorisation.mapToAuthResult(intId, None) mustBe AuthResourceNotFound(intId.get)
    }

    "return NotAuthorised when int ids dont match" in {
      val intId = Some("internal-id")
      val resource = Some("foo2")
      Authorisation.mapToAuthResult(intId, resource) mustBe NotAuthorised(intId.get)
    }
  }
}
