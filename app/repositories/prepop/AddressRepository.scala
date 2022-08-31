/*
 * Copyright 2022 HM Revenue & Customs
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

import auth.AuthorisationResource
import models.prepop.Address
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.bson.BsonRegularExpression
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, ReplaceOptions}
import play.api.libs.json.JodaWrites.JodaDateTimeWrites
import play.api.libs.json._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class AddressRepository @Inject()(mongo: MongoComponent, val configuration: ServicesConfig)(implicit ec: ExecutionContext) extends
  PlayMongoRepository[JsObject](
    mongoComponent = mongo,
    collectionName = "SharedAddresses",
    domainFormat = Address.format,
    indexes = Seq(
      IndexModel(
        ascending("registration_id", "addressLine1", "postcode", "country"),
        IndexOptions()
          .name("composite_address_index")
          .unique(true)
      ),
      IndexModel(
        ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIndex")
          .expireAfter(configuration.getInt("microservice.services.prePop.ttl"), TimeUnit.SECONDS)
      )
    )
  ) with AuthorisationResource {

  private[repositories] def now: DateTime = DateTime.now(DateTimeZone.UTC)

  private[repositories] implicit class impBsonHelpers(value: String) {
    def caseInsensitive: BsonRegularExpression = BsonRegularExpression("^" + value + "$", "i")
  }

  private def regIdSelector(regId: String): Bson = equal("registration_id", regId)

  def fetchAddresses(regId: String)(implicit ec: ExecutionContext): Future[Option[JsObject]] = {
    collection.find(regIdSelector(regId)).toFuture().map { addressList =>
      if (addressList.nonEmpty) Some(Json.obj("addresses" -> Json.toJson(addressList.map(_.-("_id")))(Writes.seq[JsObject]))) else None
    }
  }

  def insertAddress(address: JsObject)(implicit ec: ExecutionContext): Future[Boolean] =
    collection.insertOne(address).toFuture().map(_ => true)

  def updateAddress(regId: String, address: JsObject)(implicit ec: ExecutionContext): Future[Boolean] = {
    val a = address.as[Address](Address.addressReads)
    val selector =
      Filters.and(Seq(
        Some(equal("registration_id", regId.caseInsensitive)),
        Some(equal("addressLine1", a.addressLine1.caseInsensitive)),
        a.postcode.map(pc => equal("postcode", pc.caseInsensitive)),
        a.country.map(c => equal("country", c.caseInsensitive))
      ).flatten:_*)

    val updatedTTL = Json.obj("lastUpdated" -> MongoJodaFormats.dateTimeWrites.writes(now))

    collection.replaceOne(
      selector,
      address deepMerge updatedTTL,
      ReplaceOptions()
        .upsert(false)
    ).toFuture().map(_.getModifiedCount > 0)
  }

  override def getInternalId(registrationId: String)(implicit ec: ExecutionContext): Future[Option[String]] = {
    fetchAddresses(registrationId) map {
      _ flatMap { js =>
        val listOfIDs = js \\ "internal_id"
        listOfIDs.headOption.flatMap(iId =>
          if (listOfIDs.forall(_ == iId)) Some(iId.as[String]) else None)
      }
    }
  }

  override def getInternalIds(registrationId: String)(implicit ec: ExecutionContext): Future[Seq[String]] = {
    fetchAddresses(registrationId) map (_.fold[Seq[String]](Seq.empty)(addresses => (addresses \\ "internal_id").map(_.as[String])))
  }
}
