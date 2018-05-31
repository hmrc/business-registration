
package itutil

import java.time.LocalDate

import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.OneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

trait IntegrationSpecBase extends UnitSpec
  with GivenWhenThen
  with OneServerPerSuite with ScalaFutures with IntegrationPatience with Matchers
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll with DefaultAwaitTimeout {

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

  def bodyAsJson(res: Future[Result]): JsValue = Helpers.contentAsJson(res)

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

  def successfullAuth(internalId: String = testInternalId) = Json.parse(
    s"""
      |{
      | "internalId" : "$internalId"
      |}
      |""".stripMargin)

  def stubSuccessfulLogin = stubPost("/auth/authorise", 200, successfullAuth().toString())

  def stubRetrieveInternalId(internalId: String) = stubPost("/auth/authorise", 200, successfullAuth(internalId).toString())

  def stubNotLoggedIn     = stubPost("/auth/authorise", 401, "")
}