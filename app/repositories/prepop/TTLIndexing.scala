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

import play.api.Logger
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONLong}
import reactivemongo.core.commands.DeleteIndex
import uk.gov.hmrc.mongo.ReactiveRepository
import repositories.CollectionsNames.ttl
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait TTLIndexing[A, ID] {
  self: ReactiveRepository[A, ID] => // TODO - May not need
  lazy val indexs : Seq[Index] = Seq.empty
  def expireAfterSeconds : Long = ttl.toLong
  private lazy val LastUpdatedIndex = "lastUpdatedIndex"
  private lazy val EXPIRE_AFTER_SECONDS = "expireAfterSeconds"


  def ensureTTLIndexes(implicit ec: scala.concurrent.ExecutionContext): Future[Seq[Boolean]] = {

    import reactivemongo.bson.DefaultBSONHandlers._

    collection.indexesManager.list().flatMap {
      indexes => {
        val indexToUpdate = indexes.find(index =>
          index.eventualName == LastUpdatedIndex
            && index.options.getAs[BSONLong](EXPIRE_AFTER_SECONDS).getOrElse(BSONLong(expireAfterSeconds)).as[Long] != expireAfterSeconds
        )

        if (indexToUpdate.isDefined) {
          for {
            deleted <- collection.db.command(DeleteIndex(collection.name, indexToUpdate.get.eventualName))
            updated <- ensureLastUpdated
          } yield updated
        }
        else {
          ensureLastUpdated
        }
      }
    }

  }

  private def ensureLastUpdated: Future[Seq[Boolean]] = {
    Future.sequence(Seq(collection.indexesManager.ensure(
      Index(
        key = Seq("lastUpdated" -> IndexType.Ascending),
        name = Some(LastUpdatedIndex),
        options = BSONDocument(EXPIRE_AFTER_SECONDS -> BSONLong(expireAfterSeconds))
      )
    )))
  }
}
