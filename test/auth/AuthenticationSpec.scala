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

package auth

import helpers.SCRSSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.mvc.Results
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class AuthenticationSpec extends SCRSSpec {

  implicit val hc = HeaderCarrier()

  val mockAuth = mock[AuthConnector]

  object Authenticated extends Authenticated {
    val authConnector = mockAuth
  }

  def failureCase(authResult: AuthenticationResult) = Future.successful(Results.Forbidden)

//  before {
//    reset(mockAuth)
//  }

  "The authentication helper" should {

    "provided a logged in auth result when there is a valid bearer token" in {

      val internalId = "internalId"

      when(mockAuth.authorise[Option[String]](any(),any())(any(),any())).
        thenReturn(Future.successful(Some(internalId)))

      val result = Authenticated.isAuthenticated(
        failure = failureCase,
        success = { x =>
          Future.successful(Results.Ok)
      })

      val response = await(result)
      response.header.status mustBe OK
    }

    "indicate there's no logged in user where there isn't a valid bearer token" in {

      when(mockAuth.authorise[Option[String]](any(),any())(any(),any())).
        thenReturn(Future.successful(None))

      val result = Authenticated.isAuthenticated(failure = failureCase,
        success =  { x =>
        Future.successful(Results.Ok)
      })

      val response = await(result)
      response.header.status mustBe FORBIDDEN
    }
  }
}
