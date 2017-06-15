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

package services.prepop

import javax.inject.{Singleton, Inject}

import models.prepop.Address
import play.api.Logger
import play.api.libs.json.{JsArray, Reads, Json, JsObject}
import repositories.prepop.{AddressRepository, AddressRepositoryImpl}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class AddressServiceImpl @Inject()(addressesRepository: AddressRepositoryImpl) extends AddressService {
  val repository = addressesRepository.repository
}

trait AddressService {

  val repository: AddressRepository

  def fetchAddresses(registrationId: String): Future[Option[JsObject]] = {
    repository.fetchAddresses(registrationId) map { _ map { js =>
      Json.obj("addresses" -> Json.toJson((js \ "addresses").as[Seq[JsObject]] map (_.as[JsObject] - "lastUpdated" - "registration_id" - "internal_id")))
    }}
  }

  def updateAddress(registrationId: String, address: JsObject): Future[Boolean] = {
    addressExists(registrationId, address) flatMap { exists =>
      if(!exists) repository.insertAddress(registrationId, address) else repository.updateAddress(registrationId, address)
    }
  }

  private[services] def addressExists(registrationId: String, address: JsObject): Future[Boolean] = {
    val addressToUpdate = address.as[Address](Address.addressReads)
    fetchAddresses(registrationId).map(_.fold(false)(_.as[Seq[Address]](Address.listReads).exists(_.sameAs(addressToUpdate))))
  }
}
