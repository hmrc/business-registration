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

package auth

import connectors.{AuthConnector, Authority, UserIds}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Results
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class AuthorisationSpec extends UnitSpec with MockitoSugar {

  implicit val hc = HeaderCarrier()

  val mockAuth = mock[AuthConnector]
  val mockResource = mock[AuthorisationResource[String]]

  object Authorisation extends Authorisation[String] {
    val authConnector = mockAuth
    val resourceConn = mockResource
  }

//  before {
//    reset(mockAuth)
//    reset(mockResource)
//  }

  "The authorisation helper" should {

    "indicate there's no logged in user where there isn't a valid bearer token" in {

      when(mockAuth.getCurrentAuthority()(any())).thenReturn(Future.successful(None))
      when(mockResource.getInternalId(any())).thenReturn(Future.successful(None))

      val result = Authorisation.authorised("xxx") { authResult => {
          authResult shouldBe NotLoggedInOrAuthorised
          Future.successful(Results.Forbidden)
        }
      }
      val response = await(result)
      response.header.status shouldBe FORBIDDEN
    }

    "provided an authorised result when logged in and a consistent resource" in {

      val regId = "xxx"
      val userIDs = UserIds("foo", "bar")
      val a = Authority("x", "z", userIDs)

      when(mockAuth.getCurrentAuthority()(any())).thenReturn(Future.successful(Some(a)))
      when(mockResource.getInternalId(eqTo(regId))).thenReturn(Future.successful(Some((regId, userIDs.internalId))))

      val result = Authorisation.authorised(regId){ authResult => {
          authResult shouldBe Authorised(a)
          Future.successful(Results.Ok)
        }
      }
      val response = await(result)
    }

    "provided a not-authorised result when logged in and an inconsistent resource" in {

      val regId = "xxx"
      val userIDs = UserIds("foo", "bar")
      val a = Authority("x", "z", userIDs)

      when(mockAuth.getCurrentAuthority()(any())).thenReturn(Future.successful(Some(a)))
      when(mockResource.getInternalId(eqTo(regId))).thenReturn(Future.successful(Some((regId, userIDs.internalId + "xxx"))))

      val result = Authorisation.authorised(regId){ authResult => {
        authResult shouldBe NotAuthorised(a)
        Future.successful(Results.Ok)
      }
      }
      val response = await(result)
      response.header.status shouldBe OK
    }

    "provided a not-found result when logged in and no resource for the identifier" in {

      val a = Authority("x", "z", UserIds("tiid","teid"))

      when(mockAuth.getCurrentAuthority()(any())).thenReturn(Future.successful(Some(a)))
      when(mockResource.getInternalId(any())).thenReturn(Future.successful(None))

      val result = Authorisation.authorised("xxx"){ authResult => {
          authResult shouldBe AuthResourceNotFound(a)
          Future.successful(Results.Ok)
        }
      }
      val response = await(result)
      response.header.status shouldBe OK
    }
  }
}
