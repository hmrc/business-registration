
package controllers.prePop

import fixtures.MetadataFixtures
import itutil.IntegrationSpecBase
import models.Metadata
import models.prepop.ContactDetails
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.http.Status._
import repositories.MetadataMongo
import repositories.prepop.{ContactDetailsMongo, ContactDetailsRepository}
import reactivemongo.play.json._
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.ExecutionContext.Implicits.global

class ContactDetailsControllerISpec extends IntegrationSpecBase with MetadataFixtures {

  class Setup {
    lazy val contactDetailsMongo  = app.injector.instanceOf[ContactDetailsMongo]
    lazy val metadataMongo        = app.injector.instanceOf[MetadataMongo]
    lazy val authCon = app.injector.instanceOf[AuthConnector]

    val controller = new ContactDetailsController {
      override val cdRepository: ContactDetailsRepository = contactDetailsMongo.repository
      override def authConnector: AuthConnector = authCon
    }

    def dropContactDetails(internalId: String = testInternalId, regId: String = testRegistrationId) =
      await(contactDetailsMongo.repository.collection.remove(Json.obj("_id" -> regId, "InternalID" -> internalId)))
    def insertContactDetails(regId: String = testRegistrationId, intId: String = testInternalId, contact: ContactDetails = validContactDetails) =
      await(contactDetailsMongo.repository.upsertContactDetails(regId, intId, contact))
    def getContactDetails(regId: String = testRegistrationId, intId: String = testInternalId) = await(contactDetailsMongo.repository.getContactDetails(regId, intId))

    dropContactDetails()
  }

  val validContactDetails: ContactDetails = ContactDetails(
    firstName = Some("first"),
    middleName = Some("middle"),
    surname = Some("last"),
    email = Some("who@is.this"),
    telephoneNumber = Some("1234567896576"),
    mobileNumber = Some("987382728372")
  )

  val validUpdateContactJson = Json.parse(
    """
      |{
      | "firstName"  : "first",
      | "middleName" : "newMiddle",
      | "surname"    : "last"
      |}
    """.stripMargin)

  val invalidUpdateContactJson = Json.parse(
    """
      |{
      | "middleName" : true
      |}
    """.stripMargin)

  "calling getContactDetails" should {

    "return a NotFound if no data is found" in new Setup {
      stubSuccessfulLogin

      def getResponse = controller.getContactDetails(testRegistrationId)(FakeRequest())

      val result = await(getResponse)
      status(result) shouldBe NOT_FOUND
    }

    "return a 200 with a response body" in new Setup {
      stubSuccessfulLogin

      insertContactDetails()
      def getResponse = controller.getContactDetails(testRegistrationId)(FakeRequest())

      val result = await(getResponse)
      status(result) shouldBe OK
    }

    "return a Forbidden if the user is not LoggedIn" in new Setup {
      stubNotLoggedIn

      def getResponse = controller.getContactDetails(testRegistrationId)(FakeRequest())

      val result = await(getResponse)
      status(result) shouldBe FORBIDDEN
    }
  }

  "calling insertUpdateContactDetails" should {

    "return a BadRequest if an invalid json is supplied" in new Setup {
      stubSuccessfulLogin

      insertContactDetails()
      def getResponse = controller.insertUpdateContactDetails(testRegistrationId)(FakeRequest().withBody[JsValue](invalidUpdateContactJson))

      val result = await(getResponse)
      status(result) shouldBe BAD_REQUEST
    }

    "return a 200 with a response body" in new Setup {
      stubSuccessfulLogin

      insertContactDetails()
      def getResponse = controller.insertUpdateContactDetails(testRegistrationId)(FakeRequest().withBody[JsValue](validUpdateContactJson))

      val result = await(getResponse)
      status(result) shouldBe OK
      val json = getContactDetails()
      json.isEmpty shouldBe false
      json.get.middleName shouldBe Some("newMiddle")
    }

    "return a Forbidden if the user is not LoggedIn" in new Setup {
      stubNotLoggedIn

      def getResponse = controller.insertUpdateContactDetails(testRegistrationId)(FakeRequest().withBody[JsValue](Json.toJson(validNewDateTime)))

      val result = await(getResponse)
      status(result) shouldBe FORBIDDEN
    }
  }
}
