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

package repositories

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import play.api.test.Helpers._
import helpers.MongoSpec

import scala.language.postfixOps

class SequenceMongoRepositoryISpec extends MongoSpec with BeforeAndAfterAll with ScalaFutures with Eventually {

  class Setup {
    val repository = new SequenceMongoRepository(mongoComponent)
    repository.removeAll()
    await(repository.ensureIndexes)
  }

  override def afterAll(): Unit = new Setup {
    repository.removeAll()
  }

  val testSequence = "testSequence"

  "Sequence repository" should {
    "should be able to get a sequence ID" in new Setup {
      val response: Int = await(repository.getNext(testSequence))
      response mustBe 1
    }

    "get sequences, one after another from 1 to the end" in new Setup {
      val inputs: Seq[Int] = 1 to 25
      val outputs: Seq[Int] = inputs map { _ => await(repository.getNext(testSequence)) }
      outputs mustBe inputs
    }
  }
}
