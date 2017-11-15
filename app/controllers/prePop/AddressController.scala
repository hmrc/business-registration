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

package controllers.prePop

import javax.inject.{Inject, Singleton}

import auth.{Authenticated, Authorisation}
import connectors.AuthConnector
import models.prepop.Address
import play.api.libs.json._
import play.api.mvc.{Action, BodyParsers, Result}
import repositories.prepop.AddressRepositoryImpl
import services.prepop.AddressService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

@Singleton
class AddressControllerImpl @Inject()(val service: AddressService,
                                      val authConnector: AuthConnector,
                                      addressRepository: AddressRepositoryImpl) extends AddressController {
  val resourceConn = addressRepository.repository
}

trait AddressController extends BaseController with Authorisation[String] with Authenticated {

  val service: AddressService

  def fetchAddresses(registrationId: String) = Action.async {
    implicit request =>
      authorisedFor(registrationId,methodName = "fetchAddresses") { _ =>
        service.fetchAddresses(registrationId) map {
          case Some(addresses) => Ok(addresses)
          case None => NotFound
        }
      }
  }

  def updateAddress(registrationId: String) = Action.async(BodyParsers.parse.json) {
    implicit request =>
      withJsonBody[JsObject]{
        authenticatedToUpdate(registrationId, _){ address =>
          ifAddressValid(address) {
            service.updateAddress(registrationId, address) map {
              if (_) Ok else InternalServerError
            }
          }
        }
      }
  }

  private[controllers] def authenticatedToUpdate(regId: String, address: JsObject)(body: JsObject => Future[Result])
                                                (implicit hc: HeaderCarrier): Future[Result] = {
    authenticatedFor { authority =>
      val internalId = authority.ids.internalId
      resourceConn.getInternalIds(regId) flatMap { ids =>
        if (!ids.exists(_ != internalId)) {
          body(appendIDs(regId, internalId, address))
        } else {
          Future.successful(Forbidden)
        }
      }
    }
  }

  private[controllers] def appendIDs(registrationId: String, internalId: String, address: JsObject): JsObject = {
    val jsonToAppend = Json.obj("registration_id" -> registrationId, "internal_id" -> internalId)
    address ++ jsonToAppend
  }

  private[controllers] def ifAddressValid(address: JsObject)(body: => Future[Result]): Future[Result] = {
    Try(address.validate(Address.addressReads)) match {
      case Success(JsSuccess(a, _)) => if(a.isValid) body else Future.successful(BadRequest)
      case Success(JsError(errs)) => Future.successful(BadRequest)
      case Failure(ex: JsResultException) => Future.successful(BadRequest)
    }
  }
}
