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

package connectors

import java.util.UUID

import helpers.SCRSSpec
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.logging.SessionId
import play.api.test.Helpers.{BAD_REQUEST, OK}
import models.{Authority, UserIds}
import uk.gov.hmrc.play.http.{HttpGet, HttpResponse}

import scala.concurrent.Future

class AuthConnectorSpec extends SCRSSpec {

  val mockHttp: WSHttp = mock[WSHttp]
  val testAuthUrl = "testUrl"

  trait Setup {
    val connector: AuthConnector = new AuthConnector {
      val authUrl: String = testAuthUrl
      val http: HttpGet = mockHttp
    }
  }

  def authResponseJson(uri: String, userDetailsLink: String, idsLink: String): JsValue = Json.parse(
    s"""{
           "uri":"$uri",
           "userDetailsLink":"$userDetailsLink",
           "ids":"$idsLink"
        }""")

  def idsResponseJson(internalId: String, externalId: String): JsValue = Json.parse(
    s"""{
           "internalId":"$internalId",
           "externalId":"$externalId"
        }""")

  "The auth connector" should {

    val uri = "x/y/foo"
    val userDetailsLink = "bar"
    val idsLink = "/auth/ids"

    "return auth info when an authority is found" in new Setup {

      val userIDs: UserIds = UserIds("tiid", "teid")
      val expected = Authority(uri, userDetailsLink, userIDs)

      when(mockHttp.GET[HttpResponse](any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(authResponseJson(uri, userDetailsLink, idsLink)))),
                                      HttpResponse(OK, Some(idsResponseJson(userIDs.internalId, userIDs.externalId))))


      implicit val hc = defaultHC.copy(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val result = connector.getCurrentAuthority()(hc)
      val authority = await(result)

      authority shouldBe Some(expected)
    }

    "return None when an authority isn't found" in new Setup {

      when(mockHttp.GET[HttpResponse](any())(any(), any())).
        thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

      implicit val hc = defaultHC.copy(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val result = connector.getCurrentAuthority()(hc)
      val authority = await(result)

      authority shouldBe None
    }
  }

}
