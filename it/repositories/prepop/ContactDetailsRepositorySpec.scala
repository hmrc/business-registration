/*
 * Copyright 2023 HM Revenue & Customs
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

import helpers.MongoSpec
import models.prepop.{ContactDetails, MongoContactDetails, PermissionDenied}
import org.mockito.Mockito.when
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.concurrent.TimeUnit

class ContactDetailsRepositorySpec extends MongoSpec with BeforeAndAfterAll with ScalaFutures with Eventually with MockitoSugar {

  val ContactDetailsdata: ContactDetails = ContactDetails(Some("name"), Some("name"), Some("sName"), Some("email"), Some("num"), Some(""))
  val contactDetailsUpdated: ContactDetails = ContactDetails(Some("name2"), Some("name"), Some("sName"), Some("email"), Some("num"), Some("foo"))

  class Setup(allowReplaceTimeToLiveIndex: Boolean = true) {

    val timeToExpire: Int = 9999
    val mockServicesConfig = mock[ServicesConfig]

    when(mockServicesConfig.getInt("microservice.services.prePop.ttl")).thenReturn(timeToExpire)
    when(mockServicesConfig.getBoolean("mongodb.allowReplaceTimeToLiveIndex")).thenReturn(allowReplaceTimeToLiveIndex)

    val repository: ContactDetailsRepository = new ContactDetailsRepository(mongoComponent, mockServicesConfig)(executionContext)
    repository.removeAll()
    await(repository.ensureIndexes)

    def indexCount: Int = await(repository.collection.listIndexes().toFuture()).size
  }


  "upsertContactDetails" should {

    "successfully insert a contactDetails into the repository" in new Setup() {

      await(repository.upsertContactDetails("regID", "intID", ContactDetailsdata)) mustBe Some(ContactDetailsdata)
    }

    "fail to insert a contactDetails into the repository where intID is different to the records intid" in new Setup() {

      await(repository.upsertContactDetails("regID", "intID", ContactDetailsdata))
      repository.awaitCount mustBe 1
      val res: PermissionDenied = intercept[PermissionDenied] {
        await(repository.upsertContactDetails("regID", "intID1", ContactDetailsdata))
      }
      repository.awaitCount mustBe 1
    }

    "update contact details successfully" in new Setup() {

      await(repository.upsertContactDetails("regID", "intID", ContactDetailsdata))
      repository.awaitCount mustBe 1
      val res: Option[ContactDetails] = await(repository.upsertContactDetails("regID", "intID", contactDetailsUpdated))
      res mustBe Some(contactDetailsUpdated)
    }

    "update contact details succesfully not replacing valid data with blanks" in new Setup() {
      await(repository.upsertContactDetails("regID", "intID", ContactDetailsdata))
      repository.awaitCount mustBe 1
      val res: Option[ContactDetails] = await(repository.upsertContactDetails("regID", "intID", ContactDetails(Some("name1"), None, Some("name2"), None, None, None)))
      res mustBe Some(ContactDetails(Some("name1"), Some("name"), Some("name2"), Some("email"), Some("num"), Some("")))
    }

    "update Contact details should replace blank data with valid data" in new Setup() {
      await(repository.upsertContactDetails("regID", "intID", ContactDetails(Some("name1"), None, Some("sName"), None, None, None)))
      val res: Option[ContactDetails] = await(repository.upsertContactDetails("regID", "intID", ContactDetailsdata))
      res mustBe Some(ContactDetailsdata)
    }


    "should Successfully insert minimal required contact details" in new Setup() {
      val res: Option[ContactDetails] = await(repository.upsertContactDetails("regID", "intID", ContactDetails(None, None, None, None, None, None)))
      repository.awaitCount mustBe 1
      res mustBe Some(ContactDetails(None, None, None, None, None, None))
    }
  }

  "getContactDetailsUnVerifiedUser" should {

    "return permission denied when the internal id does not match the one stored in mongo" in new Setup() {
      await(repository.upsertContactDetails("reg1", "int2", ContactDetails(Some("foo"), None, Some("sName"), Some("email"), Some("num1"), Some("num2"))))
      val c: Int = repository.awaitCount
      c mustBe 1
      val res: PermissionDenied = intercept[PermissionDenied] {
        await(repository.getContactDetailsUnVerifiedUser("reg1", "int1"))
      }
      res mustBe PermissionDenied("reg1", "int1")
    }

    "return ContactDetails when internal id matches the one stored in mongo" in new Setup() {
      await(repository.upsertContactDetails("reg1", "int1", ContactDetails(Some("foo"), Some("mName"), Some("sNmee"), Some("email"), Some("num1"), Some("num2"))))
      val c: Int = repository.awaitCount
      c mustBe 1
      val res: Option[ContactDetails] = await(repository.getContactDetailsUnVerifiedUser("reg1", "int1"))
      res mustBe Some(ContactDetails(Some("foo"), Some("mName"), Some("sNmee"), Some("email"), Some("num1"), Some("num2")))
    }

    "return None when no record found in mongo" in new Setup() {
      val res: Option[ContactDetails] = await(repository.getContactDetailsUnVerifiedUser("reg1", "int1"))
      res mustBe None
    }
  }

  "getContactDetails" should {

    "return None when no record exists" in new Setup() {
      await(repository.getContactDetails("foo", "bar")) mustBe None
    }

    "return record if user has correct internal id and record exists" in new Setup() {
      await(repository.upsertContactDetails("foo", "bar", ContactDetailsdata))
      await(repository.getContactDetails("foo", "bar")) mustBe Some(ContactDetailsdata)
    }

    "return permission denied if user has incorrect internal id " in new Setup() {
      await(repository.upsertContactDetails("foo", "bar", ContactDetails(Some("foo"), None, Some(""), Some("email"), Some("num1"), Some("num2"))))
      val res: PermissionDenied = intercept[PermissionDenied] {
        await(repository.getContactDetails("foo", "bar2"))
      }
      res mustBe PermissionDenied("foo", "bar2")
    }
  }

  "getContactDetailsWithJustRegID" should {

    "get contactDetailsRecord successfully" in new Setup() {
      await(repository.upsertContactDetails("reg1", "int2", ContactDetails(Some("foo"), Some("middlenamee"), Some("sName"), Some("email"), Some("num1"), Some("num2"))))
      val res: Option[MongoContactDetails] = await(repository.getContactDetailsWithJustRegID("reg1"))
      val newRes: MongoContactDetails = res.get

      Json.toJson(newRes)(MongoContactDetails.mongoWrites).-("lastUpdated") mustBe Json.parse(
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

    "return None because nothing exists" in new Setup() {
      val res: Option[MongoContactDetails] = await(repository.getContactDetailsWithJustRegID("reg1"))
      res mustBe None
    }
  }

  "ttl index" should {

    "be applied with the correct expiration if one doesn't already exist" in new Setup() {

      await(repository.collection.dropIndexes().toFuture())

      indexCount mustBe 1 // default _id index

      await(repository.ensureIndexes)

      indexCount mustBe 2

      getTTLIndex(repository).expireAfterSeconds mustBe Some(timeToExpire)
    }

    "overwrite the current ttl index with a new one from config" in new Setup() {

      await(repository.collection.dropIndexes().toFuture())

      indexCount mustBe 1 // default _id index

      await(repository.ensureIndexes)

      indexCount mustBe 2

      await(repository.collection.dropIndex("lastUpdatedIndex").toFuture())

      indexCount mustBe 1 // dropped ttl index

      val setupTTl: Int = 1

      val setupTTLIndex: IndexModel = IndexModel(
        ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIndex")
          .expireAfter(setupTTl, TimeUnit.SECONDS)
      )

      await(repository.collection.createIndexes(Seq(setupTTLIndex)).toFuture())

      indexCount mustBe 2 // created new ttl index

      getTTLIndex(repository).expireAfterSeconds mustBe Some(setupTTl)

      await(repository.ensureIndexes)

      indexCount mustBe 2

      getTTLIndex(repository).expireAfterSeconds mustBe Some(timeToExpire)
    }

    "do nothing when ensuring the ttl index but one already exists and has the same expiration time" in new Setup() {
      await(repository.collection.dropIndexes().toFuture())

      indexCount mustBe 1 // default _id index

      await(repository.ensureIndexes)

      indexCount mustBe 2

      getTTLIndex(repository).expireAfterSeconds mustBe Some(timeToExpire)

      await(repository.ensureIndexes)

      getTTLIndex(repository).expireAfterSeconds mustBe Some(timeToExpire)
    }
  }
}
