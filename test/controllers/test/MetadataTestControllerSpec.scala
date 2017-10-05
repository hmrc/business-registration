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

package controllers.test

import helpers.SCRSControllerSpec
import org.mockito.ArgumentMatchers.any
import play.api.test.FakeRequest
import repositories.MetadataRepositoryMongo
import org.mockito.Mockito._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Request, Result}
import play.api.test.Helpers._

import scala.concurrent.Future

class MetadataTestControllerSpec extends SCRSControllerSpec {

  val mockMetadataRepository: MetadataRepositoryMongo = mock[MetadataRepositoryMongo]

  trait Setup {
    val controller: MetadataTestController = new MetadataTestController {
      val repo: MetadataRepositoryMongo = mockMetadataRepository
    }
  }

  "dropMetadataCollection" should {

    "return a 200 with a success message" in new Setup {

      when(mockMetadataRepository.drop(any())).thenReturn(Future.successful(true))

      val result: Result = await(controller.dropMetadataCollection(FakeRequest()))
      val expectedMessage: JsValue = Json.parse("""{"message":"Metadata collection dropped successfully"}""")

      status(result) shouldBe OK
      bodyAsJson(result) shouldBe expectedMessage
    }

    "return a 200 with an error message if the collection could not be dropped" in new Setup {

      when(mockMetadataRepository.drop(any())).thenReturn(Future.successful(false))

      val result: Result = await(controller.dropMetadataCollection(FakeRequest()))
      val expectedMessage: JsValue = Json.parse("""{"message":"An error occurred. Metadata collection could not be dropped"}""")

      status(result) shouldBe OK
      bodyAsJson(result) shouldBe expectedMessage
    }
  }

  "updateCompletionCapacity" should {

    "return a 200" in new Setup {

      when(mockMetadataRepository.updateCompletionCapacity(any(), any()))
        .thenReturn(Future.successful("director"))

      val request: Request[JsValue] = FakeRequest().withJsonBody(Json.parse("""{"completionCapacity" : "director"}"""))
      val result: Result = controller.updateCompletionCapacity("1234")(request)

      status(result) shouldBe 200
    }
  }
}
