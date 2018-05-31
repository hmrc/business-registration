
package controllers.prePop

import fixtures.MetadataFixtures
import itutil.IntegrationSpecBase
import models.prepop.TradingName
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import repositories.MetadataMongo
import repositories.prepop.TradingNameMongo
import play.api.http.Status._
import reactivemongo.play.json._

import scala.concurrent.ExecutionContext.Implicits.global

class TradingNameControllerISpec extends IntegrationSpecBase with MetadataFixtures {
  class Setup {
    lazy val tradingNameMongo  = app.injector.instanceOf[TradingNameMongo]
    lazy val metadataMongo        = app.injector.instanceOf[MetadataMongo]

    val controller = new TradingNameControllerImpl(tradingNameMongo)


    def dropTradingName(intId: String = testInternalId, regId: String = testRegistrationId) =
      await(tradingNameMongo.repository.collection.remove(Json.obj("_id" -> regId)))
    def insertTradingName(regId: String = testRegistrationId, intId: String = testInternalId, tradingName: String = "foo bar wizz") =
      await(tradingNameMongo.repository.collection.update(
        Json.obj("_id" -> regId, "internalId" -> intId), TradingName.mongoWrites(regId, intId).writes(tradingName), upsert = true)
      )
    def getTradingName(regId: String = testRegistrationId, intId: String = testInternalId) =
      await(tradingNameMongo.repository.collection.find(Json.obj("_id" -> regId, "internalId" -> intId)).one[JsObject].flatMap{
        case Some(x) => x.as[String](TradingName.mongoTradingNameReads)
      })

    dropTradingName()
  }

  val invalidTradingNameJson  = Json.obj("tracingName" -> "foo bar wizz")
  val validTradingNameJson    = Json.obj("tradingName" -> "foo bar wizz")

  "calling getTradingName" should {
    "return a 204 if no trading name is found" in new Setup {
      stubSuccessfulLogin
      val result = controller.getTradingName(testRegistrationId)(FakeRequest())
      status(result) shouldBe NO_CONTENT
    }

    "return a trading name if one is found" in new Setup {
      stubSuccessfulLogin
      insertTradingName()
      val result = controller.getTradingName(testRegistrationId)(FakeRequest())
      status(result) shouldBe OK
      bodyAsJson(result) shouldBe Json.obj("tradingName" -> "foo bar wizz")
    }

    "return a 403 if the user is not authorised" in new Setup {
      stubSuccessfulLogin

      insertTradingName(intId = "SomethingWrong")

      val result = controller.getTradingName(testRegistrationId)(FakeRequest())
      status(result) shouldBe FORBIDDEN
    }

    "return a 403 if the user is not logged in" in new Setup {
      stubNotLoggedIn
      val result = controller.getTradingName(testRegistrationId)(FakeRequest())
      status(result) shouldBe FORBIDDEN
    }
  }

  "calling upsertTradingName" should {
    "return the trading name that was passed in" in new Setup {
      stubSuccessfulLogin

      val result = controller.upsertTradingName(testRegistrationId)(FakeRequest().withBody(Json.obj("tradingName" -> "new foo bar wizz")))

      status(result) shouldBe OK
      bodyAsJson(result) shouldBe Json.obj("tradingName" -> "new foo bar wizz")
      getTradingName() shouldBe "new foo bar wizz"
    }

    "return a 400 for incorrect json passed in" in new Setup {
      stubSuccessfulLogin

      val result = controller.upsertTradingName(testRegistrationId)(FakeRequest().withBody(Json.obj("tracingName" -> "new foo bar wizz")))
      status(result) shouldBe BAD_REQUEST
    }

    "return a 403 if the user is not authorised" in new Setup {
      stubSuccessfulLogin

      insertTradingName(intId = "oooooops")

      val result = controller.upsertTradingName(testRegistrationId)(FakeRequest().withBody(Json.obj("tradingName" -> "new foo bar wizz")))
      status(result) shouldBe FORBIDDEN
    }

    "return a 403 if the user is not logged in" in new Setup {
      stubNotLoggedIn
      val result = controller.upsertTradingName(testRegistrationId)(FakeRequest().withBody(Json.obj("tradingName" -> "new foo bar wizz")))
      status(result) shouldBe FORBIDDEN
    }
  }
}
