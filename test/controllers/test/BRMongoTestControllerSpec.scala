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

package controllers.test

import org.mockito.Matchers
import org.scalatest.mock.MockitoSugar
import play.api.test.FakeRequest
import repositories.MetadataMongoRepository
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Mockito._
import play.api.test.Helpers._

import scala.concurrent.Future

class BRMongoTestControllerSpec extends UnitSpec with MockitoSugar {

  val mockMetadataRepository = mock[MetadataMongoRepository]

  class Setup {
    val controller = new BRMongoTestController {
      val metadataRepository = mockMetadataRepository
    }
  }

  "dropMetadataCollection" should {

    "return a 200 with a success message" in new Setup {
      when(mockMetadataRepository.drop(Matchers.any())).thenReturn(Future.successful(true))

      val result = await(controller.dropMetadataCollection(FakeRequest()))
      status(result) shouldBe OK
      jsonBodyOf(result).toString() shouldBe """{"message":"Metadata collection dropped successfully"}"""
    }

    "return a 200 with an error message if the collection could not be dropped" in new Setup {
      when(mockMetadataRepository.drop(Matchers.any())).thenReturn(Future.successful(false))

      val result = await(controller.dropMetadataCollection(FakeRequest()))
      status(result) shouldBe OK
      jsonBodyOf(result).toString() shouldBe """{"message":"An error occurred. Metadata collection could not be dropped"}"""
    }
  }
}
