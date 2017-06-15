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

package helpers

import org.scalactic.Fail
import play.api.libs.json.{Json, JsObject, JsValue}
import repositories.prepop.TTLIndexing
import uk.gov.hmrc.mongo.{ReactiveRepository, MongoSpecSupport}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext
import scala.util.Random
import scala.util.control.NoStackTrace

trait MongoSpec extends UnitSpec with MongoSpecSupport with WithFakeApplication {
  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

  def generateOID: String = {
    val alpha = "abcdef123456789"
    (1 to 24).map(x => alpha(Random.nextInt.abs % alpha.length)).mkString
  }
}

trait PimpMyRepo[T] {
  self: UnitSpec =>

  import scala.language.implicitConversions

  class MongoHelpers(repo: ReactiveRepository[T, _] with TTLIndexing[T, _]) {
    def awaitCount(implicit ex: ExecutionContext): Int = await(repo.count)
    def awaitInsert(e: T)(implicit ex: ExecutionContext) = await(repo.insert(e))
    def awaitDrop(implicit ex: ExecutionContext) = await(repo.drop)
    def awaitEnsureIndexes(implicit ex: ExecutionContext) = await(repo.ensureIndexes)
  }

  implicit def impMongoHelpers(repo: ReactiveRepository[T, _] with TTLIndexing[T, _]): MongoHelpers = new MongoHelpers(repo)

  class JsObjectHelpers(o: JsObject) {
    def pretty: String = Json.prettyPrint(o)
  }

  implicit def impJsObjectHelpers(o: JsObject): JsObjectHelpers = new JsObjectHelpers(o)

  implicit def toJsObject(v: JsValue): JsObject = v.as[JsObject]
}
