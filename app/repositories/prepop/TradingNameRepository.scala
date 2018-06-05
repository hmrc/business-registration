/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.Inject
import models.prepop.{PermissionDenied, TradingName}
import play.api.{Configuration, Logger}
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.DB
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json._
import repositories.CollectionsNames
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

class TradingNameMongo @Inject()(mongo: ReactiveMongoComponent, configuration: Configuration) {
  val repository = new TradingNameRepoMongo(mongo.mongoConnector.db, configuration)
}

trait TradingNameRepository {
  def getTradingName(regId: String, intId: String)(implicit ec: ExecutionContext): Future[Option[String]]
  def upsertTradingName(regId: String, intId: String, tradingName: String)(implicit ec: ExecutionContext): Future[Option[String]]
}

class TradingNameRepoMongo(mongo: () => DB, val configuration: Configuration)
  extends ReactiveRepository[String, BSONObjectID](collectionName = CollectionsNames.TRADING_NAME, mongo, TradingName.format)
  with TradingNameRepository with TTLIndexing[String, BSONObjectID]{

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] = for {
      ttlIndexes <- ensureTTLIndexes
      indexes    <- super.ensureIndexes
    } yield {
      indexes ++ ttlIndexes
    }

  def getTradingName(regId: String, intId: String)(implicit ec: ExecutionContext): Future[Option[String]] = fetchAndAuthorisedCheck(regId, intId)

  def upsertTradingName(regId: String, intId: String, tradingName: String)(implicit ec: ExecutionContext): Future[Option[String]] =
    fetchAndAuthorisedCheck(regId, intId).flatMap {
      _ =>
        val selector  = BSONDocument("_id" -> regId, "internalId" -> intId)
        val data      = TradingName.mongoWrites(regId, intId).writes(tradingName)
        collection.findAndUpdate(selector, data, upsert = true, fetchNewObject = true) map {
          _.result[String](TradingName.mongoTradingNameReads)
        }
    }

  private def fetchAndAuthorisedCheck(regId: String, intId: String)(implicit ec: ExecutionContext): Future[Option[String]] = {
    collection.find(BSONDocument("_id" -> regId)).one[JsObject] map {
      case Some(json) if json.as[String](TradingName.mongoInternalIdReads) != intId => throw PermissionDenied(regId, intId)
      case Some(json) => Some(json.as[String](TradingName.mongoTradingNameReads))
      case _          => None
    }
  }
}
