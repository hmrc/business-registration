
package itutil

import java.time.LocalDate

import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.OneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

trait IntegrationSpecBase extends UnitSpec
  with GivenWhenThen
  with OneServerPerSuite with ScalaFutures with IntegrationPatience with Matchers
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  val additionalConfiguration = Map(
    "microservice.services.auth.host" -> s"$mockHost",
    "microservice.services.auth.port" -> s"$mockPort",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes"
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(additionalConfiguration)
    .build

  override def beforeEach() = {
    resetWiremock()
  }

  override def beforeAll() = {
    super.beforeAll()
    startWiremock()
  }

  override def afterAll() = {
    stopWiremock()
    super.afterAll()
  }

  val testInternalId      = "TestInternalIdForITTests"
  val testRegistrationId  = "TestRegistrationIdForITTests"
  val validNewDateTime    = LocalDate.of(2010, 5, 12)

  val successfullAuth = Json.parse(
    s"""
      |{
      | "internalId" : "$testInternalId"
      |}
      |""".stripMargin)

  def stubSuccessfulLogin = stubPost("/auth/authorise", 200, successfullAuth.toString())

  def stubNotLoggedIn     = stubPost("/auth/authorise", 401, "")
}