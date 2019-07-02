
package controllers

import auth.AuthorisationResource
import fixtures.MetadataFixtures
import itutil.IntegrationSpecBase
import models.Metadata
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import reactivemongo.play.json._
import repositories.MetadataMongo
import services.{MetadataService, MetricsService}
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.ExecutionContext.Implicits.global

class MetadataControllerISpec extends IntegrationSpecBase with MetadataFixtures {

  class Setup {
    lazy val metService     = app.injector.instanceOf[MetadataService]
    lazy val metrService    = app.injector.instanceOf[MetricsService]
    lazy val metadataMongo  = app.injector.instanceOf[MetadataMongo]
    lazy val authCon        = app.injector.instanceOf[AuthConnector]

    val controller = new MetadataController {
      override val metadataService = metService
      override val metricsService = metrService
      override val resourceConn: AuthorisationResource = metadataMongo.repository
      override val authConnector: AuthConnector = authCon
    }

    def dropMetadata(internalId: String = testInternalId) = await(metadataMongo.repository.collection.remove(Json.obj("internalId" -> internalId)))
    def insertMetadata(metadata: Metadata = buildMetadata()) = await(metadataMongo.repository.createMetadata(metadata))
    def getMetadata(regId: String = testRegistrationId) = await(metadataMongo.repository.retrieveMetadata(regId))
    dropMetadata()
  }

  "calling createMetadata" should {

    "return a 201" in new Setup {
      stubSuccessfulLogin

      def getResponse = controller.createMetadata()(FakeRequest().withBody[JsValue](buildMetadataJson()))

      val result = await(getResponse)
      status(result) shouldBe CREATED
    }

    "return a 400 with an invalid Json" in new Setup {
      stubSuccessfulLogin

      def getResponse = controller.createMetadata()(FakeRequest().withBody[JsValue](Json.parse("""{}""")))

      val result = await(getResponse)
      status(result) shouldBe BAD_REQUEST
    }

    "return a Forbidden if the user is not LoggedIn" in new Setup {
      stubNotLoggedIn

      def getResponse = controller.createMetadata()(FakeRequest().withBody[JsValue](buildMetadataJson("unmatchedInternalId")))

      val result = await(getResponse)
      status(result) shouldBe FORBIDDEN
      dropMetadata("unmatchedInternalId")
    }
  }

  "calling searchMetadata" should {

    "return a NotFound if no data is found" in new Setup {
      stubSuccessfulLogin

      def getResponse = controller.searchMetadata(FakeRequest())

      val result = await(getResponse)
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 with a response body" in new Setup {
      stubSuccessfulLogin

      insertMetadata()
      def getResponse = controller.searchMetadata()(FakeRequest())

      val result = await(getResponse)
      status(result) shouldBe OK
    }

    "return a Forbidden if the user is not LoggedIn" in new Setup {
      stubNotLoggedIn

      def getResponse = controller.searchMetadata()(FakeRequest())

      val result = await(getResponse)
      status(result) shouldBe FORBIDDEN
    }
  }

  "calling retrieveMetadata" should {

    "return a NotFound if no data is found" in new Setup {
      stubSuccessfulLogin

      def getResponse = controller.retrieveMetadata(testRegistrationId)(FakeRequest())

      val result = await(getResponse)
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 with a response body" in new Setup {
      stubSuccessfulLogin

      insertMetadata()
      def getResponse = controller.retrieveMetadata(testRegistrationId)(FakeRequest())

      val result = await(getResponse)
      status(result) shouldBe OK
    }

    "return a Forbidden if the user is not LoggedIn" in new Setup {
      stubNotLoggedIn

      def getResponse = controller.retrieveMetadata(testRegistrationId)(FakeRequest())

      val result = await(getResponse)
      status(result) shouldBe FORBIDDEN
    }
  }

  "calling removeMetadata" should {

    "return a NotFound if no data is found" in new Setup {
      stubSuccessfulLogin

      def getResponse = controller.removeMetadata(testRegistrationId)(FakeRequest())

      val result = await(getResponse)
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 with a response body" in new Setup {
      stubSuccessfulLogin

      insertMetadata()
      def getResponse = controller.removeMetadata(testRegistrationId)(FakeRequest())

      val result = await(getResponse)
      status(result) shouldBe OK
    }

    "return a Forbidden if the user is not LoggedIn" in new Setup {
      stubNotLoggedIn

      def getResponse = controller.removeMetadata(testRegistrationId)(FakeRequest())

      val result = await(getResponse)
      status(result) shouldBe FORBIDDEN
    }
  }
  "calling updateMetadata" should {

    "return a NotFound if no data is found" in new Setup {
      stubSuccessfulLogin

      def getResponse = controller.updateMetaData(testRegistrationId)(FakeRequest().withBody[JsValue](buildMetadataResponseJson()))

      val result = await(getResponse)
      status(result) shouldBe NOT_FOUND
    }

    "return a BadRequest if an invalid json is supplied" in new Setup {
      stubSuccessfulLogin

      insertMetadata()
      def getResponse = controller.updateMetaData(testRegistrationId)(FakeRequest().withBody[JsValue](invalidUpdateJson))

      val result = await(getResponse)
      status(result) shouldBe BAD_REQUEST
    }

    "return a 200 with a response body" in new Setup {
      stubSuccessfulLogin

      insertMetadata()
      def getResponse = controller.updateMetaData(testRegistrationId)(FakeRequest().withBody[JsValue](buildMetadataResponseJson(cc = "Guardian")))

      val result = await(getResponse)
      status(result) shouldBe OK
      val json = getMetadata()
      json.isEmpty shouldBe false
      json.get.completionCapacity shouldBe Some("Guardian")
    }

    "return a Forbidden if the user is not LoggedIn" in new Setup {
      stubNotLoggedIn

      def getResponse = controller.updateMetaData(testRegistrationId)(FakeRequest().withBody[JsValue](buildMetadataResponseJson(cc = "Guardian")))

      val result = await(getResponse)
      status(result) shouldBe FORBIDDEN
    }
  }
  "calling updateLastSignedIn" should {

    "return a NotFound if no data is found" in new Setup {
      stubSuccessfulLogin

      def getResponse = controller.updateLastSignedIn(testRegistrationId)(FakeRequest().withBody[JsValue](Json.parse("""{"DateTime": "2010-05-12"}""")))

      val result = await(getResponse)
      status(result) shouldBe NOT_FOUND
    }

    "return a BadRequest if an invalid json is supplied" in new Setup {
      stubSuccessfulLogin

      insertMetadata()
      def getResponse = controller.updateLastSignedIn(testRegistrationId)(FakeRequest().withBody[JsValue](Json.parse("""{"DateTime": "ACE12"}""")))

      val result = await(getResponse)
      status(result) shouldBe BAD_REQUEST
    }

    "return a 200 with a response body" in new Setup {
      stubSuccessfulLogin

      insertMetadata()
      def getResponse = controller.updateLastSignedIn(testRegistrationId)(FakeRequest().withBody[JsValue](Json.toJson(validNewDateTime)))

      val result = await(getResponse)
      status(result) shouldBe OK
      val json = getMetadata()
      json.isEmpty shouldBe false
      json.get.lastSignedIn.toDateTime.dayOfMonth().get() shouldBe validNewDateTime.getDayOfMonth
      json.get.lastSignedIn.toDateTime.monthOfYear().get() shouldBe validNewDateTime.getMonthValue
      json.get.lastSignedIn.toDateTime.year().get() shouldBe validNewDateTime.getYear
    }

    "return a Forbidden if the user is not LoggedIn" in new Setup {
      stubNotLoggedIn

      def getResponse = controller.updateLastSignedIn(testRegistrationId)(FakeRequest().withBody[JsValue](Json.toJson(validNewDateTime)))

      val result = await(getResponse)
      status(result) shouldBe FORBIDDEN
    }
  }
}
