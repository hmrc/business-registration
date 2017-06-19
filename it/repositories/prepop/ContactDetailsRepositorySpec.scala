/*
 * Copyright 2017 HM Revenue & Customs
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
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.Index
import reactivemongo.bson.BSONLong
import reactivemongo.json.collection.JSONCollection
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ContactDetailsRepositorySpec extends UnitSpec with MongoSpecSupport with BeforeAndAfterAll with ScalaFutures with Eventually with WithFakeApplication {

  val ContactDetailsdata = ContactDetails(Some("name"),Some("name"),Some("sName"), Some("email"), Some("num"), Some(""))
  val contactDetailsUpdated = ContactDetails(Some("name2"),Some("name"),Some("sName"), Some("email"), Some("num"), Some("foo"))
  class Setup {
    val mongoComp = fakeApplication.injector.instanceOf[ReactiveMongoComponent]
    val repository = new ContactDetailsMongo(mongoComp).repository
    await(repository.drop)
    await(repository.ensureIndexes)

  }

"upsertContactDetails" should {
  "successfully insert a contactDetails into the repository" in new Setup {

    await(repository.upsertContactDetails("regID", "intID", ContactDetailsdata)) shouldBe Some(ContactDetailsdata)
  }
  "fail to insert a contactDetails into the repository where intID is different to the records intid" in new Setup {

    await(repository.upsertContactDetails("regID", "intID", ContactDetailsdata))
    await(repository.count) shouldBe 1
    val res =  intercept[PermissionDenied] {await(repository.upsertContactDetails("regID", "intID1", ContactDetailsdata))}
    await(repository.count) shouldBe 1
  }
  "update contact details successfully" in new Setup {

    await(repository.upsertContactDetails("regID", "intID", ContactDetailsdata))
    await(repository.count) shouldBe 1
    val res =  await(repository.upsertContactDetails("regID", "intID",contactDetailsUpdated ))
    res shouldBe Some(contactDetailsUpdated)
  }
  "update contact details succesfully not replacing valid data with blanks" in new Setup {
    await(repository.upsertContactDetails("regID", "intID", ContactDetailsdata))
    await(repository.count) shouldBe 1
    val res =  await(repository.upsertContactDetails("regID", "intID", ContactDetails(Some("name1"),None,Some("name2"),None,None,None)))
    res shouldBe Some(ContactDetails(Some("name1"),Some("name"),Some("name2"),Some("email"),Some("num"),Some("")))

  }

  "update Contact details should replace blank data with valid data" in new Setup {
    await(repository.upsertContactDetails("regID", "intID", ContactDetails(Some("name1"),None,Some("sName"),None,None,None)))
    val res = await(repository.upsertContactDetails("regID", "intID", ContactDetailsdata))
    res shouldBe Some(ContactDetailsdata)
  }


  "should Successfully insert minimal required contact details" in new Setup {
    val res = await(repository.upsertContactDetails("regID", "intID", ContactDetails(None,None,None,None,None,None)))
    await(repository.count) shouldBe 1
    res shouldBe(Some(ContactDetails(None,None,None,None,None,None)))

  }
}
  "getContactDetailsUnVerifiedUser" should {
    "return permission denied when the internal id does not match the one stored in mongo" in new Setup {
      await(repository.upsertContactDetails("reg1","int2",ContactDetails(Some("foo"),None,Some("sName"),Some("email"),Some("num1"),Some("num2"))))
      val c = await(repository.count)
      c shouldBe 1
      val res = intercept[PermissionDenied]{await(repository.getContactDetailsUnVerifiedUser("reg1","int1"))}
      res shouldBe PermissionDenied("reg1","int1")
    }

    "return ContactDetails when internal id matches the one stored in mongo" in new Setup {
      await(repository.upsertContactDetails("reg1","int1",ContactDetails(Some("foo"),Some("mName"),Some("sNmee"),Some("email"),Some("num1"),Some("num2"))))
      val c = await(repository.count)
      c shouldBe 1
      val res = await(repository.getContactDetailsUnVerifiedUser("reg1","int1"))
      res shouldBe Some(ContactDetails(Some("foo"),Some("mName"),Some("sNmee"),Some("email"),Some("num1"),Some("num2")))
    }

    "return None when no record found in mongo" in new Setup {
      val res = await(repository.getContactDetailsUnVerifiedUser("reg1","int1"))
      res shouldBe None
    }
  }
  "getContactDetails" should {
    "return None when no record exists" in new Setup {
      await(repository.getContactDetails("foo","bar")) shouldBe None
    }
    "return record if user has correct internal id and record exists" in new Setup {
      await(repository.upsertContactDetails("foo","bar",ContactDetailsdata))
      await(repository.getContactDetails("foo","bar")) shouldBe Some(ContactDetailsdata)
    }
    "return permission denied if user has incorrect internal id " in new Setup {
      await(repository.upsertContactDetails("foo","bar",ContactDetails(Some("foo"),None,Some(""),Some("email"),Some("num1"),Some("num2"))))
     val res = intercept[PermissionDenied] { await(repository.getContactDetails("foo","bar2"))}
      res shouldBe PermissionDenied("foo","bar2")
    }
  }
  //private
  "getContactDetailsWithJustRegID" should {
    "get contactDetailsRecord successfully" in new Setup {
      await(repository.upsertContactDetails("reg1", "int2", ContactDetails(Some("foo"),Some("middlenamee"),Some("sName"),Some("email"), Some("num1"), Some("num2"))))
     val res = await(repository.getContactDetailsWithJustRegID("reg1"))
      val newRes = res.get - ("lastUpdated")

      newRes shouldBe Json.parse("""{"_id":"reg1","InternalID":"int2","ContactDetails":{"firstName":"foo","middleName":"middlenamee","surname":"sName","email":"email","telephoneNumber":"num1","mobileNumber":"num2"}}""")
    }
    "return None because nothing exists" in new Setup {
      val res = await(repository.getContactDetailsWithJustRegID("reg1"))
      res shouldBe None
    }

  }
    "InternalID index" should {
      def index(col: JSONCollection): Future[Index] = col.indexesManager.list().map {
        _.filter(_.name.get == "uniqueIntID").head
      }
    "exist" in new Setup {
      index(repository.collection).futureValue.map(s => s.name).get shouldBe "uniqueIntID"
    }

    }

}
