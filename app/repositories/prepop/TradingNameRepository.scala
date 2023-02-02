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

package repositories.prepop

import models.prepop.{MongoTradingName, PermissionDenied}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model._
import repositories.CollectionsNames
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TradingNameRepository @Inject()(mongo: MongoComponent, val configuration: ServicesConfig)(implicit ec: ExecutionContext) extends
  PlayMongoRepository[MongoTradingName](
    mongo,
    collectionName = CollectionsNames.TRADING_NAME,
    MongoTradingName.mongoFormat,
    Seq(
      IndexModel(
        ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIndex")
          .expireAfter(configuration.getInt("microservice.services.prePop.ttl"), TimeUnit.SECONDS)
      )
    ),
    replaceIndexes = configuration.getBoolean("mongodb.allowReplaceTimeToLiveIndex")
  ) {

  def getTradingName(regId: String, intId: String)(implicit ec: ExecutionContext): Future[Option[String]] = fetchAndAuthorisedCheck(regId, intId)

  def upsertTradingName(regId: String, intId: String, tradingName: String)(implicit ec: ExecutionContext): Future[Option[String]] =
    fetchAndAuthorisedCheck(regId, intId).flatMap { _ =>
      val selector = Filters.and(equal("_id", regId), equal("internalId", intId))
      val data = MongoTradingName(regId, intId, tradingName)
      collection.findOneAndReplace(
        filter = selector,
        replacement = data,
        options = FindOneAndReplaceOptions()
          .upsert(true)
          .returnDocument(ReturnDocument.AFTER)
      ).headOption().map(_.map(_.tradingName))
    }

  private def fetchAndAuthorisedCheck(regId: String, intId: String)(implicit ec: ExecutionContext): Future[Option[String]] = {
    collection.find(equal("_id", regId)).headOption() map {
      case Some(tn) if tn.internalID != intId => throw PermissionDenied(regId, intId)
      case tn => tn.map(_.tradingName)
    }
  }
}
