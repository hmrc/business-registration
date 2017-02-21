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
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.play.http.{HeaderCarrier, _}
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.test.UnitSpec
import play.api.test.Helpers.{OK, BAD_REQUEST}

import scala.concurrent.Future

class AuthConnectorSpec extends SCRSSpec {

  implicit val hc = HeaderCarrier()

  val mockHttp = mock[HttpGet with HttpPost]

  val TestAuthConnector = new AuthConnector(fakeApplication, mockHttp)
//    new AuthConnector {
//    lazy val serviceUrl = "localhost"
//    val authorityUri = "auth/authority"
//    override val http: HttpGet with HttpPost = mockHttp
//  }

  def authResponseJson(uri: String, userDetailsLink: String, idsLink: String) = Json.parse(
    s"""{
           "uri":"$uri",
           "userDetailsLink":"$userDetailsLink",
           "ids":"$idsLink"
        }""")

  def idsResponseJson(internalId: String, externalId: String) = Json.parse(
    s"""{
           "internalId":"$internalId",
           "externalId":"$externalId"
        }""")

//  before {
//    reset(mockHttp)
//  }

  "The auth connector" should {
    val uri = "x/y/foo"
    val userDetailsLink = "bar"
    val idsLink = "/auth/ids"

    "return auth info when an authority is found" in {
      val userIDs: UserIds = UserIds("tiid", "teid")
      val expected = Authority(uri, userDetailsLink, userIDs)

      when(mockHttp.GET[HttpResponse](any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(authResponseJson(uri, userDetailsLink, idsLink)))),
                                      HttpResponse(OK, Some(idsResponseJson(userIDs.internalId, userIDs.externalId))))

//      when(mockHttp.GET[HttpResponse](any())(any(), any()))
//        .thenReturn(Future.successful(HttpResponse(OK, Some(idsResponseJson(userIDs.internalId, userIDs.externalId)))))

      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val result = TestAuthConnector.getCurrentAuthority()
      val authority = await(result)

      authority shouldBe Some(expected)
    }

    "return None when an authority isn't found" in {

      when(mockHttp.GET[HttpResponse](any())(any(), any())).
        thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val result = TestAuthConnector.getCurrentAuthority()
      val authority = await(result)

      authority shouldBe None
    }
  }

}
