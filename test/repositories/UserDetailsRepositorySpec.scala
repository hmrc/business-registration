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

import helpers.{SCRSSpec, MongoMocks}
import models.{Metadata, Response, WhiteListDetailsSubmit}
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.BeforeAndAfter
import reactivemongo.bson.{BSONDocument, BSONString}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.mongo.MongoSpecSupport
import TestUtilities.RandomGenerator._
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.ExecutionContext.Implicits.global

class UserDetailsRepositorySpec extends UnitSpec with MockitoSugar with MongoSpecSupport with MongoMocks with BeforeAndAfter {

  val repository = new UserDetailsRepositoryMongo(() => mockMongoDb){
    override lazy val collection = mockCollection(Some("test"))
  }
  before {
    reset(repository.collection)
  }

  "UserDetailsMongoRepository" should {

    val email = generateEmail(3,6)

    "Create a user details entry when a WhiteListDetailsSubmit model" in {

      val captor = ArgumentCaptor.forClass[WhiteListDetailsSubmit, WhiteListDetailsSubmit](classOf[WhiteListDetailsSubmit])

      val whiteListDetailsSubmit = WhiteListDetailsSubmit.empty.copy(email = email)

      setupAnyInsertOn(repository.collection, fails = false)

      val whiteListDetailsSubmitResult = await(repository.createRegistration(whiteListDetailsSubmit))

      verifyInsertOn(repository.collection, captor)

      captor.getValue.email shouldBe email

      whiteListDetailsSubmitResult.email shouldBe email
    }

    "Retrieve a user when passed an email address" in {
      val whiteListDetailsSubmitModel = mock[WhiteListDetailsSubmit]

      when(whiteListDetailsSubmitModel.email) thenReturn email

      val selector = BSONDocument("email" -> BSONString(email))
      setupFindFor(repository.collection, selector, Some(whiteListDetailsSubmitModel))

      val result = await(repository.searchRegistration(email))

      result should be(defined)
      result.get should be(whiteListDetailsSubmitModel)

      result match {
        case Some(model) => model.email shouldBe email
        case None => fail("Expected a response, got None")
      }
    }
  }
}
