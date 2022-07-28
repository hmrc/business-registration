/*
 * Copyright 2022 HM Revenue & Customs
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

package repositories.prepop

import models.prepop.{ContactDetails, PermissionDenied}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONElement, BSONLong}
import uk.gov.hmrc.mongo.{MongoSpecSupport, ReactiveRepository}

import scala.concurrent.ExecutionContext.Implicits.global

class ContactDetailsRepositorySpec extends PlaySpec with MongoSpecSupport with BeforeAndAfterAll with ScalaFutures with Eventually with GuiceOneAppPerSuite {

  val timeToExpire: Int = 9999

  val additionalConfig = Map("Test.microservice.services.prePop.ttl" -> s"$timeToExpire")

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(additionalConfig)
    .build()

  val ContactDetailsdata: ContactDetails = ContactDetails(Some("name"), Some("name"), Some("sName"), Some("email"), Some("num"), Some(""))
  val contactDetailsUpdated: ContactDetails = ContactDetails(Some("name2"), Some("name"), Some("sName"), Some("email"), Some("num"), Some("foo"))

  class Setup {
    val repository: ContactDetailsRepository = app.injector.instanceOf[ContactDetailsRepository]
    await(repository.removeAll())
    await(repository.ensureIndexes)

    def indexCount: Int = await(repository.collection.indexesManager.list).size
  }


  "upsertContactDetails" should {

    "successfully insert a contactDetails into the repository" in new Setup {

      await(repository.upsertContactDetails("regID", "intID", ContactDetailsdata)) mustBe Some(ContactDetailsdata)
    }

    "fail to insert a contactDetails into the repository where intID is different to the records intid" in new Setup {

      await(repository.upsertContactDetails("regID", "intID", ContactDetailsdata))
      await(repository.count) mustBe 1
      val res: PermissionDenied = intercept[PermissionDenied] {
        await(repository.upsertContactDetails("regID", "intID1", ContactDetailsdata))
      }
      await(repository.count) mustBe 1
    }

    "update contact details successfully" in new Setup {

      await(repository.upsertContactDetails("regID", "intID", ContactDetailsdata))
      await(repository.count) mustBe 1
      val res: Option[ContactDetails] = await(repository.upsertContactDetails("regID", "intID", contactDetailsUpdated))
      res mustBe Some(contactDetailsUpdated)
    }

    "update contact details succesfully not replacing valid data with blanks" in new Setup {
      await(repository.upsertContactDetails("regID", "intID", ContactDetailsdata))
      await(repository.count) mustBe 1
      val res: Option[ContactDetails] = await(repository.upsertContactDetails("regID", "intID", ContactDetails(Some("name1"), None, Some("name2"), None, None, None)))
      res mustBe Some(ContactDetails(Some("name1"), Some("name"), Some("name2"), Some("email"), Some("num"), Some("")))
    }

    "update Contact details should replace blank data with valid data" in new Setup {
      await(repository.upsertContactDetails("regID", "intID", ContactDetails(Some("name1"), None, Some("sName"), None, None, None)))
      val res: Option[ContactDetails] = await(repository.upsertContactDetails("regID", "intID", ContactDetailsdata))
      res mustBe Some(ContactDetailsdata)
    }


    "should Successfully insert minimal required contact details" in new Setup {
      val res: Option[ContactDetails] = await(repository.upsertContactDetails("regID", "intID", ContactDetails(None, None, None, None, None, None)))
      await(repository.count) mustBe 1
      res mustBe Some(ContactDetails(None, None, None, None, None, None))
    }
  }

  "getContactDetailsUnVerifiedUser" should {

    "return permission denied when the internal id does not match the one stored in mongo" in new Setup {
      await(repository.upsertContactDetails("reg1", "int2", ContactDetails(Some("foo"), None, Some("sName"), Some("email"), Some("num1"), Some("num2"))))
      val c: Int = await(repository.count)
      c mustBe 1
      val res: PermissionDenied = intercept[PermissionDenied] {
        await(repository.getContactDetailsUnVerifiedUser("reg1", "int1"))
      }
      res mustBe PermissionDenied("reg1", "int1")
    }

    "return ContactDetails when internal id matches the one stored in mongo" in new Setup {
      await(repository.upsertContactDetails("reg1", "int1", ContactDetails(Some("foo"), Some("mName"), Some("sNmee"), Some("email"), Some("num1"), Some("num2"))))
      val c: Int = await(repository.count)
      c mustBe 1
      val res: Option[ContactDetails] = await(repository.getContactDetailsUnVerifiedUser("reg1", "int1"))
      res mustBe Some(ContactDetails(Some("foo"), Some("mName"), Some("sNmee"), Some("email"), Some("num1"), Some("num2")))
    }

    "return None when no record found in mongo" in new Setup {
      val res: Option[ContactDetails] = await(repository.getContactDetailsUnVerifiedUser("reg1", "int1"))
      res mustBe None
    }
  }

  "getContactDetails" should {

    "return None when no record exists" in new Setup {
      await(repository.getContactDetails("foo", "bar")) mustBe None
    }

    "return record if user has correct internal id and record exists" in new Setup {
      await(repository.upsertContactDetails("foo", "bar", ContactDetailsdata))
      await(repository.getContactDetails("foo", "bar")) mustBe Some(ContactDetailsdata)
    }

    "return permission denied if user has incorrect internal id " in new Setup {
      await(repository.upsertContactDetails("foo", "bar", ContactDetails(Some("foo"), None, Some(""), Some("email"), Some("num1"), Some("num2"))))
      val res: PermissionDenied = intercept[PermissionDenied] {
        await(repository.getContactDetails("foo", "bar2"))
      }
      res mustBe PermissionDenied("foo", "bar2")
    }
  }

  "getContactDetailsWithJustRegID" should {

    "get contactDetailsRecord successfully" in new Setup {
      await(repository.upsertContactDetails("reg1", "int2", ContactDetails(Some("foo"), Some("middlenamee"), Some("sName"), Some("email"), Some("num1"), Some("num2"))))
      val res: Option[JsObject] = await(repository.getContactDetailsWithJustRegID("reg1"))
      val newRes: JsObject = res.get - "lastUpdated"

      newRes mustBe Json.parse(
        """{
          |"_id":"reg1",
          |"InternalID":"int2",
          |"ContactDetails":{
          |"firstName":"foo",
          |"middleName":"middlenamee",
          |"surname":"sName",
          |"email":"email",
          |"telephoneNumber":"num1",
          |"mobileNumber":"num2"
          |}
          |}""".stripMargin)
    }

    "return None because nothing exists" in new Setup {
      val res: Option[JsObject] = await(repository.getContactDetailsWithJustRegID("reg1"))
      res mustBe None
    }
  }

  "ttl index" should {

    def getTTLIndex(repo: ReactiveRepository[_, _]): Index = {
      await(repo.collection.indexesManager.list).filter(_.eventualName == "lastUpdatedIndex").head
    }

    "be applied with the correct expiration if one doesn't already exist" in new Setup {

      await(repository.collection.indexesManager.dropAll())

      indexCount mustBe 1 // default _id index

      await(repository.ensureIndexes)

      indexCount mustBe 2

      val indexes: List[Index] = await(repository.collection.indexesManager.list)

      indexes.exists(_.eventualName == "lastUpdatedIndex") mustBe true
      getTTLIndex(repository).options.elements mustBe Stream(BSONElement("expireAfterSeconds", BSONLong(timeToExpire)))
    }

    "overwrite the current ttl index with a new one from config" in new Setup {

      await(repository.collection.indexesManager.dropAll())

      indexCount mustBe 1 // default _id index

      await(repository.ensureIndexes)

      indexCount mustBe 2

      await(repository.collection.indexesManager.drop("lastUpdatedIndex"))

      indexCount mustBe 1 // dropped ttl index

      val setupTTl: Int = 1

      val setupTTLIndex: Index = Index(
        key = Seq("lastUpdated" -> IndexType.Ascending),
        name = Some("lastUpdatedIndex"),
        options = BSONDocument("expireAfterSeconds" -> BSONLong(setupTTl))
      )

      await(repository.collection.indexesManager.create(setupTTLIndex))

      indexCount mustBe 2 // created new ttl index

      val indexes: List[Index] = await(repository.collection.indexesManager.list)

      indexes.exists(_.eventualName == "lastUpdatedIndex") mustBe true
      getTTLIndex(repository).options.elements mustBe Stream(BSONElement("expireAfterSeconds", BSONLong(setupTTl)))

      await(repository.ensureIndexes)

      indexCount mustBe 2

      indexes.exists(_.eventualName == "lastUpdatedIndex") mustBe true
      getTTLIndex(repository).options.elements mustBe Stream(BSONElement("expireAfterSeconds", BSONLong(timeToExpire)))
    }

    "do nothing when ensuring the ttl index but one already exists and has the same expiration time" in new Setup {
      await(repository.collection.indexesManager.dropAll())

      indexCount mustBe 1 // default _id index

      await(repository.ensureIndexes)

      indexCount mustBe 2

      val indexes: List[Index] = await(repository.collection.indexesManager.list)

      indexes.exists(_.eventualName == "lastUpdatedIndex") mustBe true
      getTTLIndex(repository).options.elements mustBe Stream(BSONElement("expireAfterSeconds", BSONLong(timeToExpire)))

      await(repository.ensureIndexes)

      indexes.exists(_.eventualName == "lastUpdatedIndex") mustBe true
      getTTLIndex(repository).options.elements mustBe Stream(BSONElement("expireAfterSeconds", BSONLong(timeToExpire)))
    }
  }
}
