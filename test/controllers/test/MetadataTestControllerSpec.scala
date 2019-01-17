/*
 * Copyright 2019 HM Revenue & Customs
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

import helpers.SCRSSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import repositories.{MetadataMongo, MetadataRepository, MetadataRepositoryMongo}

import scala.concurrent.Future

class MetadataTestControllerSpec extends SCRSSpec {

  val mockMetadataRepository = mock[MetadataMongo]
  val mockrepo = mock[MetadataRepositoryMongo]


  class Setup {
    val controller = new BRMongoTestController {
      override val repo: MetadataRepository = mockrepo
    }
  }

  "dropMetadataCollection" should {

    "return a 200 with a success message" in new Setup {
      when(mockrepo.drop(any())).thenReturn(Future.successful(true))
      val result = controller.dropMetadataCollection(FakeRequest())
      status(result) mustBe OK
      bodyAsJson(result).toString() mustBe """{"message":"Metadata collection dropped successfully"}"""
    }

    "return a 200 with an error message if the collection could not be dropped" in new Setup {
      when(mockrepo.drop(any())).thenReturn(Future.successful(false))
      val result = controller.dropMetadataCollection(FakeRequest())
      status(result) mustBe OK
      bodyAsJson(result).toString() mustBe """{"message":"An error occurred. Metadata collection could not be dropped"}"""
    }
  }

  "updateCC" should {
    "return a 200" in new Setup {
      when(mockrepo.updateCompletionCapacity(any(), any())(any()))
        .thenReturn(Future.successful("director"))

      val result = controller.updateCompletionCapacity("1234")(FakeRequest().withBody[JsValue](Json.parse("""{"completionCapacity" : "director"}""")))
      status(result) mustBe OK
    }
  }
}
