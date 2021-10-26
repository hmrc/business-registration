/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.libs.json.JodaWrites.JodaDateTimeWrites
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson._
import reactivemongo.play.json.ImplicitBSONHandlers.BSONDocumentWrites
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class AddressRepository @Inject()(mongo: ReactiveMongoComponent, val configuration: ServicesConfig) extends ReactiveRepository[JsObject, BSONObjectID](
  collectionName = "SharedAddresses",
  mongo = mongo.mongoConnector.db,
  domainFormat = Address.format
) with AuthorisationResource with TTLIndexing[JsObject, BSONObjectID] {

  private[repositories] def now: DateTime = DateTime.now(DateTimeZone.UTC)

  private[repositories] implicit class impBsonHelpers(value: String) {
    def caseInsensitive: BSONRegex = BSONRegex("^" + value + "$", "i")
  }

  override def indexes: Seq[Index] = {
    Seq(
      Index(
        key = Seq("registration_id" -> IndexType.Ascending,
          "addressLine1" -> IndexType.Ascending,
          "postcode" -> IndexType.Ascending,
          "country" -> IndexType.Ascending),
        name = Some("composite_address_index"), unique = true
      )
    )
  }

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] = {
    super.ensureIndexes flatMap { l =>
      ensureTTLIndexes map {
        ttl => l ++ ttl
      }
    }
  }

  private def regIdSelector(regId: String): (String, Json.JsValueWrapper) = "registration_id" -> regId

  def fetchAddresses(regId: String)(implicit ec: ExecutionContext): Future[Option[JsObject]] = {
    find(regIdSelector(regId)) map { addressList =>
      if (addressList.nonEmpty) Some(Json.obj("addresses" -> Json.toJson(addressList.map(_.-("_id")))(Writes.list[JsObject]))) else None
    }
  }

  private def fetchAddress(regId: String, selector: BSONDocument)(implicit ec: ExecutionContext): Future[Option[JsObject]] = {
    collection.find(selector).one[JsObject]
  }

  def insertAddress(regId: String, address: JsObject)(implicit ec: ExecutionContext): Future[Boolean] = {
    collection.insert(address)(Address.mongoWrites, ec).map(_.writeErrors.isEmpty)
  }

  def updateAddress(regId: String, address: JsObject)(implicit ec: ExecutionContext): Future[Boolean] = {
    val a = address.as[Address](Address.addressReads)
    val selector = BSONDocument("registration_id" -> regId.caseInsensitive) ++
      BSONDocument("addressLine1" -> a.addressLine1.caseInsensitive) ++
      a.postcode.fold(BSONDocument())(pc => BSONDocument("postcode" -> pc.caseInsensitive)) ++
      a.country.fold(BSONDocument())(c => BSONDocument("country" -> c.caseInsensitive))

    val updatedTTL = Json.obj("lastUpdated" -> JodaDateTimeWrites.writes(now))

    fetchAddress(regId, selector) flatMap { existingAddressOpt =>
      existingAddressOpt.fold(Future.successful(false)) { existingAddress =>
        val updatedAddress = address deepMerge updatedTTL
        collection.update(selector, updatedAddress)(implicitly[OWrites[BSONDocument]], Address.mongoWrites, ec)
          .map { err => err.writeErrors.isEmpty }
      }
    }
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
