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

package services.prepop

import javax.inject.{Inject, Singleton}
import models.prepop.Address
import play.api.libs.json.{JsObject, Json}
import repositories.prepop.AddressRepository

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class AddressService @Inject()(addressesRepository: AddressRepository) {

  def fetchAddresses(registrationId: String)(implicit ec: ExecutionContext): Future[Option[JsObject]] = {
    addressesRepository.fetchAddresses(registrationId) map {
      _ map { js =>
        Json.obj("addresses" -> Json.toJson((js \ "addresses").as[Seq[JsObject]] map (_.as[JsObject] - "lastUpdated" - "registration_id" - "internal_id")))
      }
    }
  }

  def updateAddress(registrationId: String, address: JsObject)(implicit ec: ExecutionContext): Future[Boolean] = {
    addressExists(registrationId, address) flatMap { exists =>
      if (!exists) addressesRepository.insertAddress(address) else addressesRepository.updateAddress(registrationId, address)
    }
  }

  private[services] def addressExists(registrationId: String, address: JsObject)(implicit ec: ExecutionContext): Future[Boolean] = {
    val addressToUpdate = address.as[Address](Address.addressReads)
    fetchAddresses(registrationId).map(_.fold(false)(_.as[Seq[Address]](Address.listReads).exists(_.sameAs(addressToUpdate))))
  }
}
