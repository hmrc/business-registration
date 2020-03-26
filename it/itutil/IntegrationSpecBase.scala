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

package itutil

import java.time.LocalDate

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.{DefaultAwaitTimeout, Helpers}

import scala.concurrent.Future

trait IntegrationSpecBase extends PlaySpec
  with GivenWhenThen
  with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll with DefaultAwaitTimeout {

  val mockHost: String = WiremockHelper.wiremockHost
  val mockPort: Int = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  val additionalConfiguration: Map[String, String] = Map(
    "microservice.services.auth.host" -> s"$mockHost",
    "microservice.services.auth.port" -> s"$mockPort",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes"
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(additionalConfiguration)
    .build

  def bodyAsJson(res: Future[Result]): JsValue = Helpers.contentAsJson(res)

  override def beforeEach(): Unit = {
    resetWiremock()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  val testInternalId: String = "TestInternalIdForITTests"
  val testRegistrationId: String = "TestRegistrationIdForITTests"
  val validNewDateTime: LocalDate = LocalDate.of(2010, 5, 12)

  def successfullAuth(internalId: String = testInternalId): JsValue = Json.parse(
    s"""
       |{
       | "internalId" : "$internalId"
       |}
       |""".stripMargin)

  def stubSuccessfulLogin: StubMapping = stubPost("/auth/authorise", 200, successfullAuth().toString())

  def stubRetrieveInternalId(internalId: String): StubMapping = stubPost("/auth/authorise", 200, successfullAuth(internalId).toString())

  def stubNotLoggedIn: StubMapping = stubPost("/auth/authorise", 401, "")
}