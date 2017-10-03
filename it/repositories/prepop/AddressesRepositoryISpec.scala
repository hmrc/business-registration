/*
* Copyright 2016 HM Revenue & Customs
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

import helpers.{MongoSpec, RichReactiveRepository}
import models.prepop.Address
import org.joda.time.{DateTimeZone, DateTime}
import org.scalactic.Fail
import play.api.Logger
import play.api.libs.json.{Reads, Json, JsObject}
import reactivemongo.bson.BSONObjectID
import reactivemongo.core.errors.DatabaseException
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.util.Random

class AddressesRepositoryISpec extends MongoSpec with RichReactiveRepository {

  class Setup {
    val repo = new AddressMongoRepository(mongo)

    repo.awaitDrop
    await(repo.ensureIndexes)
    repo.awaitCount shouldBe 0
  }

  val regId = "reg-12345"
  val regId1 = "regId1"
  val regId2 = "regId2"

  val dateTime = DateTime.parse("2017-06-15T10:06:28.434Z")
  val now = Json.toJson(dateTime)(ReactiveMongoFormats.dateTimeWrite)

  def buildAddressJson(regId: String, withOid: Boolean = true, invalid: Boolean = false, different: Boolean = false): JsObject = {

    val rId = if(different) generateOID.take(3) else regId

    val addressJson = Json.parse(
      s"""
         |{
         |  "registration_id" : "$regId",
         |  "addressLine1" : "testAddressLine1-$rId",
         |  "addressLine2" : "testAddressLine2-$rId",
         |  "addressLine3" : "testAddressLine3-$rId",
         |  "addressLine4" : "testAddressLine4-$rId",
         |  "postcode" : "testPostcode-$rId",
         |  "country" : "testCountry-$rId",
         |  "lastUpdated" : $now
         |}
    """.stripMargin).as[JsObject] ++
      (if(withOid) Json.parse(s"""{"_id" : {"$$oid" : "$generateOID"}}""") else Json.obj())

    if(invalid) addressJson.-("postcode").-("country") else addressJson
  }

  case class FetchOptions(regId: String, withOid: Boolean = true, invalid: Boolean = false, different: Boolean = false)

  def buildFetchedAddressJson(options: Seq[FetchOptions]): JsObject = {
    Json.obj("addresses" -> Json.toJson((options map (option => buildAddressJson(
      option.regId, withOid = option.withOid, invalid = option.invalid, different = option.different
    )))
      .map(_.-("_id"))))
  }

  "fetchAddresses" should {

    "return a single address" in new Setup {

      val expectedJson = Json.parse(
        s"""
          |{
          |  "addresses" : [
          |    {
          |      "registration_id" : "regId1",
          |      "addressLine1" : "testAddressLine1-regId1",
          |      "addressLine2" : "testAddressLine2-regId1",
          |      "addressLine3" : "testAddressLine3-regId1",
          |      "addressLine4" : "testAddressLine4-regId1",
          |      "postcode" : "testPostcode-regId1",
          |      "country" : "testCountry-regId1",
          |      "lastUpdated" : $now
          |    }
          |  ]
          |}
        """.stripMargin).as[JsObject]

      repo.awaitInsert(buildAddressJson(regId1))
      repo.awaitCount shouldBe 1

      val result = await(repo.fetchAddresses(regId1))
      result.get shouldBe expectedJson
    }

    "return multiple addresses with the same regId" in new Setup {

      val expectedJson = Json.parse(
        s"""
          |{
          |  "addresses" : [
          |    {
          |      "registration_id" : "regId1",
          |      "addressLine1" : "testAddressLine1-regId1",
          |      "addressLine2" : "testAddressLine2-regId1",
          |      "addressLine3" : "testAddressLine3-regId1",
          |      "addressLine4" : "testAddressLine4-regId1",
          |      "postcode" : "testPostcode-regId1",
          |      "country" : "testCountry-regId1",
          |      "lastUpdated" : $now
          |    },
          |    {
          |      "registration_id" : "regId1",
          |      "addressLine1" : "testAddressLine1-regId12",
          |      "addressLine2" : "testAddressLine2-regId12",
          |      "addressLine3" : "testAddressLine3-regId12",
          |      "addressLine4" : "testAddressLine4-regId12",
          |      "postcode" : "testPostcode-regId12",
          |      "country" : "testCountry-regId12",
          |      "lastUpdated" : $now
          |    }
          |  ]
          |}
        """.stripMargin).as[JsObject]

      repo.awaitInsert(buildAddressJson(regId1))
      repo.awaitCount shouldBe 1

      repo.awaitInsert(Json.parse(
        s"""
          |{
          |  "registration_id" : "regId1",
          |  "addressLine1" : "testAddressLine1-regId12",
          |  "addressLine2" : "testAddressLine2-regId12",
          |  "addressLine3" : "testAddressLine3-regId12",
          |  "addressLine4" : "testAddressLine4-regId12",
          |  "postcode" : "testPostcode-regId12",
          |  "country" : "testCountry-regId12",
          |  "lastUpdated" : $now
          |}
        """.stripMargin))

      repo.awaitCount shouldBe 2

      val result = await(repo.fetchAddresses(regId1))
      result.get.pretty shouldBe expectedJson.pretty
    }

    "return 1 address when multiple exist but only 1 is associated with the supplied regId" in new Setup {
      val expectedJson = Json.parse(
        s"""
          |{
          |  "addresses" : [
          |    {
          |      "registration_id" : "regId1",
          |      "addressLine1" : "testAddressLine1-regId1",
          |      "addressLine2" : "testAddressLine2-regId1",
          |      "addressLine3" : "testAddressLine3-regId1",
          |      "addressLine4" : "testAddressLine4-regId1",
          |      "postcode" : "testPostcode-regId1",
          |      "country" : "testCountry-regId1",
          |      "lastUpdated" : $now
          |    }
          |  ]
          |}
        """.stripMargin)


      repo.awaitInsert(buildAddressJson(regId1))
      repo.awaitCount shouldBe 1

      repo.awaitInsert(buildAddressJson("2"))
      repo.awaitCount shouldBe 2

      val result = await(repo.fetchAddresses(regId1))
      result shouldBe Some(expectedJson)
    }

    "return no addresses when multiple exist but none are associated with the supplied regId" in new Setup {

      repo.awaitInsert(buildAddressJson("2"))
      repo.awaitCount shouldBe 1

      repo.awaitInsert(buildAddressJson("3"))
      repo.awaitCount shouldBe 2

      val result = await(repo.fetchAddresses(regId1))
      result shouldBe None
    }
  }

  "insertAddress" should {

    "insert an address" in new Setup {

      val result = await(repo.insertAddress(regId1, buildAddressJson(regId1)))
      result shouldBe true

      repo.awaitCount shouldBe 1
    }

    "throw a DatabaseException when inserting a duplicate Address" in new Setup {
      await(repo.insertAddress(regId1, buildAddressJson(regId1, withOid = false)))
      repo.awaitCount shouldBe 1

      val ex = intercept[DatabaseException](await(repo.insertAddress(regId1, buildAddressJson(regId1, withOid = false))))

      ex.code shouldBe Some(11000)

    }
  }

  "updateAddress" should {
    import scala.language.implicitConversions

    implicit class addressImp(o: JsObject) {
      def withoutTTL: JsObject = o - "lastUpdated"
      def withoutObjectID: JsObject = o - "_id"
      def getAddressesAsList: Seq[JsObject] = (o \ "addresses").as[Seq[JsObject]](Reads.seq(Address.reads))
      def getTTL: DateTime = (o \ "lastUpdated").as[DateTime](ReactiveMongoFormats.dateTimeRead)
    }

    "update the lastUpdated ttl value when the supplied address exactly matches an existing address for the regId" in new Setup {
      val existingAddress = buildAddressJson(regId)
      val suppliedAddress = buildAddressJson(regId, withOid = false)

      val originalTTL = (existingAddress \ "lastUpdated").as[DateTime](ReactiveMongoFormats.dateTimeRead)
      val oid = (existingAddress \ "_id").as[BSONObjectID](ReactiveMongoFormats.objectIdRead)

      repo.awaitInsert(existingAddress)
      repo.awaitCount shouldBe 1

      val result = await(repo.updateAddress(regId, suppliedAddress))

      result shouldBe true
      repo.awaitCount shouldBe 1

      val updatedAddress: JsObject = await(repo.findById(oid)).get
      val updatedTTL = (updatedAddress \ "lastUpdated").as[DateTime](ReactiveMongoFormats.dateTimeRead)

      updatedAddress - "lastUpdated" - "_id" shouldBe suppliedAddress - "lastUpdated"

      updatedTTL isAfter originalTTL shouldBe true
    }

    "update an existing addresses fields if the supplied address is equal but non-equality checked fields have changed" in new Setup {
      val existingAddress = buildAddressJson(regId)
      val oid = (existingAddress \ "_id").as[BSONObjectID](ReactiveMongoFormats.objectIdRead)
      val newAddressLine2 = "newAddressLine2"
      val suppliedAddressWithNewAddressLine2 = buildAddressJson(regId, withOid = false) + ("addressLine2" -> Json.toJson(newAddressLine2))

      repo.awaitInsert(existingAddress)
      repo.awaitCount shouldBe 1

      val result = await(repo.updateAddress(regId, suppliedAddressWithNewAddressLine2))

      result shouldBe true
      repo.awaitCount shouldBe 1

      val updatedAddress = await(repo.findById(oid)).get

      updatedAddress.withoutTTL.withoutObjectID shouldBe suppliedAddressWithNewAddressLine2.withoutTTL
    }

    "update an existing addresses fields if the supplied address is equal but the cases are different" in new Setup {
      val dateTimeNow = now

      val existingAddress = Json.parse(
        s"""{
            |  "_id" : {"$$oid" : "$generateOID"},
            |  "registration_id" : "$regId",
            |  "addressLine1" : "testAddressLine1",
            |  "addressLine2" : "testAddressLine2",
            |  "addressLine3" : "testAddressLine3",
            |  "addressLine4" : "testAddressLine4",
            |  "postcode" : "testPostcode",
            |  "country" : "testCountry",
            |  "lastUpdated" : $dateTimeNow
            |}
            |""".stripMargin).as[JsObject]

      val oid = (existingAddress \ "_id").as[BSONObjectID](ReactiveMongoFormats.objectIdRead)

      val suppliedAddress = Json.parse(
        s"""{
            |  "registration_id" : "$regId",
            |  "addressLine1" : "TESTADDRESSLINE1",
            |  "addressLine2" : "testAddressLine2",
            |  "addressLine3" : "testAddressLine3",
            |  "addressLine4" : "testAddressLine4",
            |  "postcode" : "TESTPOSTCODE",
            |  "country" : "TESTCOUNTRY"
            |}
            |""".stripMargin).as[JsObject]

      repo.awaitInsert(existingAddress)
      repo.awaitCount shouldBe 1

      val result = await(repo.updateAddress(regId, suppliedAddress))

      result shouldBe true

      repo.awaitCount shouldBe 1

      val updatedAddress = await(repo.findById(oid)).get

      existingAddress.getTTL isBefore updatedAddress.getTTL shouldBe true

      updatedAddress.withoutTTL.withoutObjectID shouldBe suppliedAddress.withoutTTL
    }
  }
}
