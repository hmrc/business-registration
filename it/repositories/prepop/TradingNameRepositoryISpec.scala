
package repositories.prepop

import models.prepop.PermissionDenied
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Configuration}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONElement, BSONLong}
import uk.gov.hmrc.mongo.{MongoSpecSupport, ReactiveRepository}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global

class TradingNameRepositoryISpec extends UnitSpec with MongoSpecSupport with BeforeAndAfterAll
  with ScalaFutures with Eventually with WithFakeApplication {


  val timeToExpire: Int = 9999

  val additionalConfig = Map("Test.microservice.services.prePop.ttl" -> s"$timeToExpire")

  override lazy val fakeApplication: Application = new GuiceApplicationBuilder()
    .bindings(bindModules:_*)
    .configure(additionalConfig)
    .build()

  implicit val app: Application = fakeApplication

  class Setup {
    private val config    = fakeApplication.injector.instanceOf(classOf[Configuration])
    private val mongoComp = fakeApplication.injector.instanceOf[ReactiveMongoComponent]
    val repository: TradingNameRepoMongo = new TradingNameMongo(mongoComp, config).repository
    await(repository.drop)
    await(repository.ensureIndexes)

    def indexCount: Int = await(repository.collection.indexesManager.list).size
    def count: Int = await(repository.count)
  }

  "upsertTradingName" should {
    "add the trading name if none exists" in new Setup {
      count shouldBe 0
      await(repository.upsertTradingName("regId", "intId", "my trading name"))
      count shouldBe 1
    }

    "return Forbidden if user is not authorised" in new Setup {
      count shouldBe 0
      await(repository.upsertTradingName("regId", "intId", "my trading name"))
      count shouldBe 1
      intercept[PermissionDenied](await(repository.upsertTradingName("regId", "wrongIntId", "my trading name")))
    }

    "update trading name if it exists" in new Setup {
      count shouldBe 0
      await(repository.upsertTradingName("regId", "intId", "my trading name"))
      count shouldBe 1
      val res = await(repository.upsertTradingName("regId", "intId", "my new trading name"))
      count shouldBe 1
      res shouldBe Some("my new trading name")
    }
  }

  "getTradingName" should {
    "return none if nothing exists" in new Setup {
      count shouldBe 0
      await(repository.getTradingName("regId", "intId")) shouldBe None
    }

    "return the trading name if one exists" in new Setup {
      count shouldBe 0
      await(repository.upsertTradingName("regId", "intId", "my trading name"))
      count shouldBe 1
      await(repository.getTradingName("regId", "intId")) shouldBe Some("my trading name")
    }

    "return Forbidden if user is not authorised" in new Setup {
      count shouldBe 0
      await(repository.upsertTradingName("regId", "intId", "my trading name"))
      count shouldBe 1
      intercept[PermissionDenied](await(repository.getTradingName("regId", "wrongIntId")))
    }
  }

  "ttl index" should {

    def getTTLIndex(repo: ReactiveRepository[_, _]): Index = {
      await(repo.collection.indexesManager.list).filter(_.eventualName == "lastUpdatedIndex").head
    }

    "be applied with the correct expiration if one doesn't already exist" in new Setup {

      await(repository.collection.indexesManager.dropAll())

      indexCount shouldBe 1 // default _id index

      await(repository.ensureIndexes)

      indexCount shouldBe 2

      val indexes: List[Index] = repository.collection.indexesManager.list

      indexes.exists(_.eventualName == "lastUpdatedIndex") shouldBe true
      getTTLIndex(repository).options.elements shouldBe Stream(BSONElement("expireAfterSeconds", BSONLong(timeToExpire)))
    }

    "overwrite the current ttl index with a new one from config" in new Setup {

      await(repository.collection.indexesManager.dropAll())

      indexCount shouldBe 1 // default _id index

      await(repository.ensureIndexes)

      indexCount shouldBe 2

      await(repository.collection.indexesManager.drop("lastUpdatedIndex"))

      indexCount shouldBe 1 // dropped ttl index

      val setupTTl: Int = 1

      val setupTTLIndex = Index(
        key = Seq("lastUpdated" -> IndexType.Ascending),
        name = Some("lastUpdatedIndex"),
        options = BSONDocument("expireAfterSeconds" -> BSONLong(setupTTl))
      )

      await(repository.collection.indexesManager.create(setupTTLIndex))

      indexCount shouldBe 2 // created new ttl index

      val indexes: List[Index] = repository.collection.indexesManager.list

      indexes.exists(_.eventualName == "lastUpdatedIndex") shouldBe true
      getTTLIndex(repository).options.elements shouldBe Stream(BSONElement("expireAfterSeconds", BSONLong(setupTTl)))

      await(repository.ensureIndexes)

      indexCount shouldBe 2

      indexes.exists(_.eventualName == "lastUpdatedIndex") shouldBe true
      getTTLIndex(repository).options.elements shouldBe Stream(BSONElement("expireAfterSeconds", BSONLong(timeToExpire)))
    }

    "do nothing when ensuring the ttl index but one already exists and has the same expiration time" in new Setup {
      await(repository.collection.indexesManager.dropAll())

      indexCount shouldBe 1 // default _id index

      await(repository.ensureIndexes)

      indexCount shouldBe 2

      val indexes: List[Index] = repository.collection.indexesManager.list

      indexes.exists(_.eventualName == "lastUpdatedIndex") shouldBe true
      getTTLIndex(repository).options.elements shouldBe Stream(BSONElement("expireAfterSeconds", BSONLong(timeToExpire)))

      await(repository.ensureIndexes)

      indexes.exists(_.eventualName == "lastUpdatedIndex") shouldBe true
      getTTLIndex(repository).options.elements shouldBe Stream(BSONElement("expireAfterSeconds", BSONLong(timeToExpire)))
    }
  }

}
