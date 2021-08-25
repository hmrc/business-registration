/*
 * Copyright 2021 HM Revenue & Customs
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

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.JsValue
import play.api.mvc.{AnyContentAsJson, Request, Result}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits, Helpers, ResultExtractors}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait SCRSSpec extends PlaySpec
  with MockitoSugar
  with BeforeAndAfterEach
  with FutureAwaits
  with DefaultAwaitTimeout
  with ResultExtractors
  with HeaderNames
  with Status {

  implicit val defaultHC: HeaderCarrier = HeaderCarrier()
  implicit val defaultEC: ExecutionContext = ExecutionContext.global.prepare()

  private val FIVE = 5L
  private implicit val timeout: Timeout = Timeout(FIVE, TimeUnit.SECONDS)

  def bodyAsJson(res: Future[Result]): JsValue = Helpers.contentAsJson(res)

  implicit def anyContentAsJsonToJsValue(r: Request[AnyContentAsJson]): Request[JsValue] = r.map(_.json)
}
