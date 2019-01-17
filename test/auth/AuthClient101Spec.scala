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

import helpers.AuthMocks
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.mvc.{Action, Controller}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthClient101Spec extends UnitSpec with MockitoSugar with AuthMocks {

  implicit val hc = HeaderCarrier()

  object TestController extends Controller with AuthorisedFunctions {
    val authConnector: AuthConnector = mockAuthConnector

    def loginFunction = Action.async {
      authorised() {
        Future.successful(Ok("dfs"))
      } recover {
        case _ => Forbidden
      }
    }

    def getInternalId = Action.async {
      authorised().retrieve(internalId) {
        x => Future.successful(x.fold(NotFound)(id => Ok))
      } recover {
        case _ => Forbidden
      }
    }

  }

  "Controller call" should {
    "return a forbidden if user is not authorised" in {
      when(mockAuthConnector.authorise[Unit](ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future(throw new Exception))
      val result = TestController.loginFunction(FakeRequest())
      status(result) shouldBe Status.FORBIDDEN
    }
    "return 200 if user is authorised" in {
      when(mockAuthConnector.authorise[Unit](ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future())

      val result = TestController.loginFunction(FakeRequest())
      status(result) shouldBe Status.OK
    }
    "return not found if no internal id is present" in {
      when(mockAuthConnector.authorise[Option[String]](ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future(None))

      val result = TestController.getInternalId(FakeRequest())
      status(result) shouldBe Status.NOT_FOUND
    }
    "return ok if internal id is present" in {
      when(mockAuthConnector.authorise[Option[String]](ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future(Some("internalId")))

      val result = TestController.getInternalId(FakeRequest())
      status(result) shouldBe Status.OK
    }
  }

}
