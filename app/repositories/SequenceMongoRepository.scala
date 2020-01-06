/*
 * Copyright 2020 HM Revenue & Customs
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

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import models.Metadata
import play.api.Application
import play.api.libs.json.{Format, JsValue}
import reactivemongo.api.DB
import reactivemongo.bson._
import reactivemongo.play.json._
import repositories.CollectionsNames.SEQUENCE
import repositories.InjectDB.injectDB
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}


@ImplementedBy(classOf[SequenceRepositoryImpl])
trait SequenceRepository {
  def getNext(sequence: String)(implicit ec: ExecutionContext): Future[Int]
}

abstract class SequenceRepositoryBase(mongo: () => DB)(implicit formats: Format[Metadata], manifest: Manifest[Metadata])
  extends ReactiveRepository[Metadata, BSONObjectID](SEQUENCE, mongo, formats)
    with SequenceRepository

@Singleton
class SequenceRepositoryImpl @Inject()(implicit app: Application) extends SequenceRepositoryBase(injectDB(app)) {

  def getNext(sequence: String)(implicit ec: ExecutionContext): Future[Int] = {
    val selector = BSONDocument("_id" -> sequence)
    val modifier = BSONDocument("$inc" -> BSONDocument("seq" -> 1))

    collection.findAndUpdate(selector, modifier, fetchNewObject = true, upsert = true)
      .map {
        _.result[JsValue] match {
          case None => -1
          case Some(x) => (x \ "seq").as[Int]
        }
      }
  }
}
