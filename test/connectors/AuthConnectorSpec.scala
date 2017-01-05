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

import org.mockito.Mockito._
import org.mockito.Matchers
import org.scalatest.{BeforeAndAfter, ShouldMatchers, WordSpecLike}
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeApplication
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.{HeaderCarrier, _}
import uk.gov.hmrc.play.http.logging.SessionId

import scala.concurrent.Future

class AuthConnectorSpec extends FakeApplication with WordSpecLike with ShouldMatchers with MockitoSugar with BeforeAndAfter {

  implicit val hc = HeaderCarrier()

  val mockHttp = mock[HttpGet with HttpPost]

  object TestAuthConnector extends AuthConnector {
    lazy val serviceUrl = "localhost"
    val authorityUri = "auth/authority"
    override val http: HttpGet with HttpPost = mockHttp
  }

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

  before {
    reset(mockHttp)
  }

  "The auth connector" should {
    val uri = "x/y/foo"
    val userDetailsLink = "bar"
    val idsLink = "/auth/ids"

    "return auth info when an authority is found" in {
      val userIDs: UserIds = UserIds("tiid", "teid")
      val expected = Authority(uri, userDetailsLink, userIDs)

      when(mockHttp.GET[HttpResponse](Matchers.eq("localhost/auth/authority"))(Matchers.any(), Matchers.any())).
        thenReturn(Future.successful(HttpResponse(200, Some(authResponseJson(uri, userDetailsLink, idsLink)))))

      when(mockHttp.GET[HttpResponse](Matchers.eq(s"localhost${idsLink}"))(Matchers.any(), Matchers.any())).
        thenReturn(Future.successful(HttpResponse(200, Some(idsResponseJson(userIDs.internalId, userIDs.externalId)))))

      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val result = TestAuthConnector.getCurrentAuthority()
      val authority = await(result)

      authority shouldBe Some(expected)
    }

    "return None when an authority isn't found" in {

      when(mockHttp.GET[HttpResponse](Matchers.eq("localhost/auth/authority"))(Matchers.any(), Matchers.any())).
        thenReturn(Future.successful(HttpResponse(404, None)))

      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val result = TestAuthConnector.getCurrentAuthority()
      val authority = await(result)

      authority shouldBe None
    }
  }

}
