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
package repositories

import models.{Response, WhiteListDetailsSubmit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.ExecutionContext.Implicits.global

class UserDetailsMongoRepositoryISpec extends UnitSpec with MongoSpecSupport with BeforeAndAfterAll with ScalaFutures with Eventually with WithFakeApplication {


  class Setup {
    val repo = fakeApplication.injector.instanceOf(classOf[UserDetailsMongo]).repository

    await(repo.drop)
    await(repo.ensureIndexes)
    count shouldBe 0

    def count = await(repo.count)
  }

  override def afterAll() = new Setup {
    await(repo.drop)
  }

  def insertMultipleDetails(repo: UserDetailsRepositoryMongo) = {
    await(repo.createRegistration(whiteListDetails))
    await(repo.createRegistration(whiteListDetails.copy(email = "some@other.email", firstName = "newName", lastName = "iDidIt")))
    await(repo.count) shouldBe 2
  }

  val whiteListDetails = WhiteListDetailsSubmit(
    firstName       = "testFirstName",
    lastName        = "testLastName",
    phone           = "12345678987",
    email           = "test@email.com",
    affinityGroup   = "testAffinityGroup",
    submissionTime  = DateTimeUtils.now
  )

  "createRegistration" should {
    "return the details passed in if successful" in new Setup {
      val result = await(repo.createRegistration(whiteListDetails))

      result shouldBe whiteListDetails
      count shouldBe 1
    }
  }

  "search registration" should {
    "return a single record if found" in new Setup {
      insertMultipleDetails(repo)

      val result = await(repo.searchRegistration("test@email.com"))

      result.isDefined shouldBe true
      result.get.firstName shouldBe "testFirstName"
      result.get.lastName shouldBe "testLastName"
    }

    "return nothing if no record is found" in new Setup {
      insertMultipleDetails(repo)
      val result = await(repo.searchRegistration("doesnt@exist.com"))
      result.isDefined shouldBe false
    }
  }

  "remove BetaUsers" should {
    "return a dropped response if successful" in new Setup {
      insertMultipleDetails(repo)

      val result = await(repo.removeBetaUsers)

      result.isDefined shouldBe true
      result.get.resp shouldBe "Dropped"
      count shouldBe 0
    }
  }
}
