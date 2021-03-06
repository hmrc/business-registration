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

package repositories.prepop

import play.api.{Configuration, Logger}
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONLong}
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

trait TTLIndexing[A, ID] {
  self: ReactiveRepository[A, ID] =>

  val configuration: Configuration

  lazy val ttl: Long = configuration.getLong("microservice.services.prePop.ttl").getOrElse(throw new Exception("Can't find key prePop.ttl"))

  private val colName: String = self.collection.name

  private val LAST_UPDATED_INDEX = "lastUpdatedIndex"
  private val EXPIRE_AFTER_SECONDS = "expireAfterSeconds"

  def ensureTTLIndexes(implicit ec: scala.concurrent.ExecutionContext): Future[Seq[Boolean]] = {

    collection.indexesManager.list().flatMap {
      indexes => {

        val ttlIndex: Option[Index] = indexes.find(_.eventualName == LAST_UPDATED_INDEX)

        ttlIndex match {
          case Some(index) if hasSameTTL(index) =>
            Logger.info(s"[TTLIndex] document expiration value for collection : $colName has not been changed")
            doNothing
          case Some(index) =>
            Logger.info(s"[TTLIndex] document expiration value for collection : $colName has been changed. Updating ttl index to : $ttl")
            deleteIndex(index) flatMap (_ => ensureLastUpdated)
          case _ =>
            Logger.info(s"[TTLIndex] TTL Index for collection : $colName does not exist. Creating TTL index")
            ensureLastUpdated
        }
      }
    } recoverWith errorHandler
  }

  private def doNothing(implicit ec: ExecutionContext) = Future(Seq(true))

  private def hasSameTTL(index: Index): Boolean = index.options.getAs[BSONLong](EXPIRE_AFTER_SECONDS).exists(_.as[Long] == ttl)

  private def deleteIndex(index: Index)(implicit ec: ExecutionContext): Future[Int] = collection.indexesManager.drop(index.eventualName).map { amountDropped =>
    Logger.info(s"[deleteIndex] dropped $amountDropped for ${index.eventualName}")
    amountDropped
  }

  private def errorHandler: PartialFunction[Throwable, Future[Seq[Boolean]]] = {
    case ex =>
      Logger.error(s"[TTLIndex] Exception thrown in TTLIndexing", ex)
      throw ex
  }

  private def ensureLastUpdated(implicit ec: ExecutionContext): Future[Seq[Boolean]] = {
    Future.sequence(Seq(collection.indexesManager.ensure(
      Index(
        key = Seq("lastUpdated" -> IndexType.Ascending),
        name = Some(LAST_UPDATED_INDEX),
        options = BSONDocument(EXPIRE_AFTER_SECONDS -> BSONLong(ttl))
      )
    ))).map { ensured =>
      Logger.info(s"[TTLIndex] Ensuring ttl index on field : $LAST_UPDATED_INDEX in collection : $colName is set to $ttl")
      ensured
    }
  }
}
