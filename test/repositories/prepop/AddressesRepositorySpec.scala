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

import helpers.MongoMocks
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddressesRepositorySpec extends UnitSpec with MockitoSugar with MongoMocks {

  class Setup {
    val repo = new AddressMongoRepository(() => mockMongoDb) {
      override lazy val collection = mockCollection(Some(""))
      override def ensureIndexes(implicit ec: scala.concurrent.ExecutionContext): Future[Seq[Boolean]] = Future.successful(Seq(true))
    }

    reset(repo.collection)
  }

  val regId = "reg-12345"
  val internalID = "int-12345"

  "getInternalId" should {

    val fetchedJson = Json.parse(
      s"""
        |{
        |  "registration_id":"$regId",
        |  "internal_id":"$internalID"
        |}
      """.stripMargin)

    val fetchedJsonDifferentInternalID = Json.parse(
      s"""
         |{
         |  "registration_id":"$regId",
         |  "internal_id":"otherInternalID"
         |}
      """.stripMargin)

    "return a regId and internal id" when {

      "a single address is fetched" in new Setup {
        setupFindFor(repo.collection, Seq(fetchedJson))

        await(repo.getInternalId(regId)) shouldBe Some((regId, internalID))
      }

      "multiple addresses are fetched with the same regId and internal Id" in new Setup {
        setupFindFor(repo.collection, Seq(fetchedJson, fetchedJson, fetchedJson))

        await(repo.getInternalId(regId)) shouldBe Some((regId, internalID))
      }
    }

    "return none" when {

      "no addresses are fetched" in new Setup {
        val json = Json.parse("""{}""")
        setupFindFor(repo.collection, Seq(json))

        await(repo.getInternalId(regId)) shouldBe None
      }

      "multiple addresses are fetched with the same regId but different internal Id's" in new Setup {
        setupFindFor(repo.collection, Seq(fetchedJson, fetchedJsonDifferentInternalID))

        await(repo.getInternalId(regId)) shouldBe None
      }
    }
  }
}
