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
import models.prepop.Address
import org.mongodb.scala.MongoException
import org.mongodb.scala.bson.ObjectId
import play.api.libs.json.{JsObject, JsValue, Json, Reads}
import play.api.test.Helpers._
import uk.gov.hmrc.mongo.play.json.formats.{MongoFormats, MongoJavatimeFormats}

import java.time.Instant

class AddressesRepositoryISpec extends MongoSpec {

  class Setup {
    val repo: AddressRepository = app.injector.instanceOf(classOf[AddressRepository])

    repo.removeAll()
    await(repo.ensureIndexes)
    repo.awaitCount mustBe 0
  }

  val regId = "reg-12345"
  val regId1 = "regId1"
  val regId2 = "regId2"

  val dateTime: Instant = Instant.parse("2017-06-15T10:06:28.434Z")
  val now: JsValue = Json.toJson(dateTime)(MongoJavatimeFormats.instantWrites)

  def buildAddressJson(regId: String, withOid: Boolean = true, invalid: Boolean = false, different: Boolean = false): JsObject = {

    val rId = if (different) generateOID.take(3) else regId

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
      (if (withOid) Json.parse(s"""{"_id" : {"$$oid" : "$generateOID"}}""") else Json.obj())

    if (invalid) addressJson.-("postcode").-("country") else addressJson
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

      val expectedJson: JsObject = Json.parse(
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
      repo.awaitCount mustBe 1

      val result: Option[JsObject] = await(repo.fetchAddresses(regId1))
      result.get mustBe expectedJson
    }

    "return multiple addresses with the same regId" in new Setup {

      val expectedJson: JsObject =
        Json.obj("addresses" -> Json.arr(
          Json.obj("registration_id" -> "regId1",
            "addressLine1" -> "testAddressLine1-regId1",
            "addressLine2" -> "testAddressLine2-regId1",
            "addressLine3" -> "testAddressLine3-regId1",
            "addressLine4" -> "testAddressLine4-regId1",
            "postcode" -> "testPostcode-regId1",
            "country" -> "testCountry-regId1",
            "lastUpdated" -> now
          ), Json.obj(
            "registration_id" -> "regId1",
            "addressLine1" -> "testAddressLine1-regId12",
            "addressLine2" -> "testAddressLine2-regId12",
            "addressLine3" -> "testAddressLine3-regId12",
            "addressLine4" -> "testAddressLine4-regId12",
            "postcode" -> "testPostcode-regId12",
            "country" -> "testCountry-regId12",
            "lastUpdated" -> now
          )
        ))

      repo.awaitInsert(buildAddressJson(regId1))
      repo.awaitCount mustBe 1

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

      repo.awaitCount mustBe 2

      val result: Option[JsObject] = await(repo.fetchAddresses(regId1))
      result.get mustBe expectedJson
    }

    "return 1 address when multiple exist but only 1 is associated with the supplied regId" in new Setup {
      val expectedJson: JsValue = Json.parse(
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
      repo.awaitCount mustBe 1

      repo.awaitInsert(buildAddressJson("2"))
      repo.awaitCount mustBe 2

      val result: Option[JsObject] = await(repo.fetchAddresses(regId1))
      result mustBe Some(expectedJson)
    }

    "return no addresses when multiple exist but none are associated with the supplied regId" in new Setup {

      repo.awaitInsert(buildAddressJson("2"))
      repo.awaitCount mustBe 1

      repo.awaitInsert(buildAddressJson("3"))
      repo.awaitCount mustBe 2

      val result: Option[JsObject] = await(repo.fetchAddresses(regId1))
      result mustBe None
    }
  }

  "insertAddress" should {

    "insert an address" in new Setup {

      val result: Boolean = await(repo.insertAddress(buildAddressJson(regId1)))
      result mustBe true

      repo.awaitCount mustBe 1
    }

    "throw a DatabaseException when inserting a duplicate Address" in new Setup {
      await(repo.insertAddress(buildAddressJson(regId1, withOid = false)))
      repo.awaitCount mustBe 1

      val ex: MongoException = intercept[MongoException](await(repo.insertAddress(buildAddressJson(regId1, withOid = false))))

      ex.getCode mustBe 11000

    }
  }

  "updateAddress" should {
    import scala.language.implicitConversions

    implicit class addressImp(o: JsObject) {
      def withoutTTL: JsObject = o - "lastUpdated"

      def withoutObjectID: JsObject = o - "_id"

      def getAddressesAsList: Seq[JsObject] = (o \ "addresses").as[Seq[JsObject]](Reads.seq(Address.reads))

      def getTTL: Instant = (o \ "lastUpdated").as[Instant](MongoJavatimeFormats.instantReads)
    }

    "update the lastUpdated ttl value when the supplied address exactly matches an existing address for the regId" in new Setup {
      val existingAddress: JsObject = buildAddressJson(regId)
      val suppliedAddress: JsObject = buildAddressJson(regId, withOid = false)

      val originalTTL: Instant = (existingAddress \ "lastUpdated").as[Instant](MongoJavatimeFormats.instantReads)
      val oid: ObjectId = (existingAddress \ "_id").as[ObjectId](MongoFormats.objectIdFormat)

      repo.awaitInsert(existingAddress)
      repo.awaitCount mustBe 1

      val result: Boolean = await(repo.updateAddress(regId, suppliedAddress))

      result mustBe true
      repo.awaitCount mustBe 1

      val updatedAddress: JsObject = repo.findById(oid).get
      val updatedTTL: Instant = (updatedAddress \ "lastUpdated").as[Instant](MongoJavatimeFormats.instantReads)

      updatedAddress - "lastUpdated" - "_id" mustBe suppliedAddress - "lastUpdated"

      updatedTTL isAfter originalTTL mustBe true
    }

    "update an existing addresses fields if the supplied address is equal but non-equality checked fields have changed" in new Setup {
      val existingAddress: JsObject = buildAddressJson(regId)
      val oid: ObjectId = (existingAddress \ "_id").as[ObjectId](MongoFormats.objectIdFormat)
      val newAddressLine2 = "newAddressLine2"
      val suppliedAddressWithNewAddressLine2: JsObject = buildAddressJson(regId, withOid = false) + ("addressLine2" -> Json.toJson(newAddressLine2))

      repo.awaitInsert(existingAddress)
      repo.awaitCount mustBe 1

      val result: Boolean = await(repo.updateAddress(regId, suppliedAddressWithNewAddressLine2))

      result mustBe true
      repo.awaitCount mustBe 1

      val updatedAddress: JsObject = repo.findById(oid).get

      updatedAddress.withoutTTL.withoutObjectID mustBe suppliedAddressWithNewAddressLine2.withoutTTL
    }

    "update an existing addresses fields if the supplied address is equal but the cases are different" in new Setup {
      val dateTimeNow: JsValue = now

      val existingAddress: JsObject = Json.parse(
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

      val oid: ObjectId = (existingAddress \ "_id").as[ObjectId](MongoFormats.objectIdFormat)

      val suppliedAddress: JsObject = Json.parse(
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
      repo.awaitCount mustBe 1

      val result: Boolean = await(repo.updateAddress(regId, suppliedAddress))

      result mustBe true

      repo.awaitCount mustBe 1

      val updatedAddress: JsObject = repo.findById(oid).get

      existingAddress.getTTL isBefore updatedAddress.getTTL mustBe true

      updatedAddress.withoutTTL.withoutObjectID mustBe suppliedAddress.withoutTTL
    }

    "update with country and postcode missing" in new Setup {
      val dateTimeNow: JsValue = now

      val existingAddress: JsObject = Json.parse(
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

      val oid: ObjectId = (existingAddress \ "_id").as[ObjectId](MongoFormats.objectIdFormat)

      val suppliedAddress: JsObject = Json.parse(
        s"""{
           |  "registration_id" : "$regId",
           |  "addressLine1" : "TESTADDRESSLINE1",
           |  "addressLine2" : "testAddressLine2",
           |  "addressLine3" : "testAddressLine3",
           |  "addressLine4" : "testAddressLine4"
           |}
           |""".stripMargin).as[JsObject]

      repo.awaitInsert(existingAddress)
      repo.awaitCount mustBe 1

      val result: Boolean = await(repo.updateAddress(regId, suppliedAddress))

      result mustBe true

      repo.awaitCount mustBe 1

      val updatedAddress: JsObject = repo.findById(oid).get

      existingAddress.getTTL isBefore updatedAddress.getTTL mustBe true

      updatedAddress.withoutTTL.withoutObjectID mustBe suppliedAddress.withoutTTL
    }
  }

  "getInternalId" should {
    "return a single internalId if present" in new Setup {
      repo.awaitInsert(buildAddressJson(regId) ++ Json.parse("""{"internal_id":"testInternalId"}"""))
      val internalIdResponse: Option[String] = await(repo.getInternalId(regId))
      internalIdResponse mustBe defined
      internalIdResponse mustBe Some("testInternalId")
    }

    "none if not present" in new Setup {
      await(repo.getInternalId(regId)) mustBe None
    }
  }

  "getInternalIds" should {
    "return a single internalId if present" in new Setup {
      repo.awaitInsert(buildAddressJson(regId) ++ Json.parse("""{"internal_id":"testInternalId"}"""))
      val internalIdResponse: Seq[String] = await(repo.getInternalIds(regId))
      internalIdResponse mustBe Seq("testInternalId")
    }

    "none if not present" in new Setup {
      await(repo.getInternalIds(regId)) mustBe Seq()
    }
  }
}
