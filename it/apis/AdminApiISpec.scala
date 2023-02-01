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

package apis

import helpers.MongoSpec
import models.Metadata
import org.mongodb.scala.result.InsertOneResult
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.test.Helpers._
import repositories.MetadataMongoRepository

import java.time.Instant

class AdminApiISpec extends MongoSpec with GuiceOneServerPerSuite {

  override lazy val app: Application = fakeApplication()

  trait Setup {
    val metadataMongoRepository: MetadataMongoRepository = app.injector.instanceOf[MetadataMongoRepository]
    metadataMongoRepository.removeAll()
    await(metadataMongoRepository.ensureIndexes)

    def count: Int = metadataMongoRepository.awaitCount

    def insert(e: Metadata): InsertOneResult = await(metadataMongoRepository.collection.insertOne(e).toFuture())
  }

  val regId = "reg-id-12345"
  val internalId = "int-id-12345"
  val formCreationTS = "2017-08-09T17:10:30+01:00"

  val ws: WSClient = app.injector.instanceOf(classOf[WSClient])

  def client(path: String): WSRequest = ws.url(s"http://localhost:$port/business-registration/admin$path").withFollowRedirects(false)

  "GET /admin/business-tax-registration/:registrationID" should {

    val path = s"/business-tax-registration/$regId"

    val metadata = Metadata(internalId, regId, formCreationTS, "ENG", None, Some("director"),
      declareAccurateAndComplete = false, Instant.parse("2017-08-09T17:10:30Z"))

    "return a 200 and a business registration document as json when one is found for the supplied regId" in new Setup {

      insert(metadata)
      count mustBe 1

      val response: WSResponse = await(client(path).get())

      val expected: JsValue = Json.parse(
        s"""
           |{
           |  "registrationID":"$regId",
           |  "formCreationTimestamp":"$formCreationTS",
           |  "language":"ENG",
           |  "completionCapacity":"director",
           |  "links":{
           |    "self":"/business-registration/business-tax-registration/$regId"
           |  }
           |}
      """.stripMargin)

      response.status mustBe 200
      response.json mustBe expected
    }

    "return a 404 when a business registration document is not found with the supplied regId" in new Setup {

      count mustBe 0

      val response: WSResponse = await(client(path).get())

      response.status mustBe 404
    }
  }
}
