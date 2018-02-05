
package controllers.prePop

import fixtures.MetadataFixtures
import itutil.IntegrationSpecBase
import models.prepop.ContactDetails
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.FakeRequest
import play.api.http.Status._
import repositories.MetadataMongo
import repositories.prepop.{AddressRepositoryImpl, ContactDetailsMongo}
import reactivemongo.play.json._
import services.prepop.{AddressService, AddressServiceImpl}

import scala.concurrent.ExecutionContext.Implicits.global

class AddressControllerISpec extends IntegrationSpecBase {

  class Setup {
    lazy val addressService  = app.injector.instanceOf[AddressServiceImpl]
    lazy val addressMongo        = app.injector.instanceOf[AddressRepositoryImpl]

    val controller = new AddressControllerImpl(addressService, addressMongo)


    def dropAddress(regId: String = testRegistrationId) =
      await(addressMongo.repository.collection.remove(Json.obj("registration_id" -> regId)))
    def insertAddress(regId: String = testRegistrationId, address: JsObject = validAddressJson ++ validIdsJson) =
      await(addressMongo.repository.insertAddress(regId, address))
    def getAddresses(regId: String = testRegistrationId) = await(addressMongo.repository.fetchAddresses(regId))

    dropAddress()
  }

  val validAddressJson = Json.parse(
    s"""
      |{
      | "addressLine1": "woooop",
      | "postcode": "weeee",
      | "country" : "noon12"
      |}
    """.stripMargin).as[JsObject]

  val validIdsJson = Json.obj("registration_id" -> testRegistrationId, "internal_id" -> testInternalId)

  val invalidAddressJson = Json.parse(
    """
      |{
      | "postcode": "weeee",
      | "country" : "noon12"
      |}
    """.stripMargin).as[JsObject]

  val validUpdateAddressJson = Json.parse(
    """
      |{
      | "addressLine1": "newLine1",
      |  "postcode": "newPostCode",
      | "country" : "newCountry"
      |}
    """.stripMargin)


  "calling getAddressDetails" should {

    "return a NotFound if no data is found" in new Setup {
      stubSuccessfulLogin

      def getResponse = controller.fetchAddresses(testRegistrationId)(FakeRequest())

      val result = await(getResponse)
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 with a response body" in new Setup {
      stubSuccessfulLogin

      insertAddress()
      def getResponse = controller.fetchAddresses(testRegistrationId)(FakeRequest())

      val result = await(getResponse)
      status(result) shouldBe OK
    }

    "return a Forbidden if the user is not LoggedIn" in new Setup {
      stubNotLoggedIn

      def getResponse = controller.fetchAddresses(testRegistrationId)(FakeRequest())

      val result = await(getResponse)
      status(result) shouldBe FORBIDDEN
    }
  }

  "calling updateAddress" should {

    "return a BadRequest if an invalid json is supplied" in new Setup {
      stubSuccessfulLogin

      insertAddress()
      def getResponse = controller.updateAddress(testRegistrationId)(FakeRequest().withBody[JsValue](invalidAddressJson))

      val result = await(getResponse)
      status(result) shouldBe BAD_REQUEST
    }

    "return a 200 with a response body" in new Setup {
      stubSuccessfulLogin

      insertAddress()
      def getResponse = controller.updateAddress(testRegistrationId)(FakeRequest().withBody[JsValue](validUpdateAddressJson))

      val result = await(getResponse)
      status(result) shouldBe OK
      val json = getAddresses()
      json.isEmpty shouldBe false
      ((json.get \ "addresses").as[Seq[JsValue]].head \ "addressLine1").asOpt[String] shouldBe Some("newLine1")
    }

    "return a Forbidden if the user is not LoggedIn" in new Setup {
      stubNotLoggedIn

      def getResponse = controller.updateAddress(testRegistrationId)(FakeRequest().withBody[JsValue](validUpdateAddressJson))

      val result = await(getResponse)
      status(result) shouldBe FORBIDDEN
    }
  }
}
