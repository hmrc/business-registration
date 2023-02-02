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

package controllers.prePop

import auth.Authorisation
import controllers.helper.AuthControllerHelpers
import models.prepop.Address
import play.api.libs.json._
import play.api.mvc._
import repositories.prepop.AddressRepository
import services.prepop.AddressService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@Singleton
class AddressController @Inject()(addressService: AddressService,
                                  val resourceConn: AddressRepository,
                                  val authConnector: AuthConnector,
                                  controllerComponents: ControllerComponents
                                 ) extends BackendController(controllerComponents) with Authorisation with AuthControllerHelpers {

  def fetchAddresses(registrationId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(registrationId)(
        failure = authorisationFailureResultHandler("fetchAddresses"),
        success = {
          addressService.fetchAddresses(registrationId) map {
            case Some(addresses) => Ok(addresses)
            case None => NotFound
          }
        })
  }

  def updateAddress(registrationId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      withJsonBody[JsObject] {
        authenticatedToUpdate(registrationId, _) { address =>
          ifAddressValid(address) {
            addressService.updateAddress(registrationId, address) map {
              if (_) Ok else InternalServerError
            }
          }
        }
      }
  }

  private[controllers] def authenticatedToUpdate(regId: String, address: JsObject)(body: JsObject => Future[Result])
                                                (implicit hc: HeaderCarrier): Future[Result] = {
    isAuthenticated(
      failure = authenticationFailureResultHandler("authenticatedToUpdate"),
      success = { internalId =>
        resourceConn.getInternalIds(regId) flatMap { ids =>
          if (!ids.exists(_ != internalId)) {
            body(appendIDs(regId, internalId, address))
          } else {
            Future.successful(Forbidden)
          }
        }
      })
  }

  private[controllers] def appendIDs(registrationId: String, internalId: String, address: JsObject): JsObject = {
    val jsonToAppend = Json.obj("registration_id" -> registrationId, "internal_id" -> internalId)
    address ++ jsonToAppend
  }

  private[controllers] def ifAddressValid(address: JsObject)(body: => Future[Result]): Future[Result] = {
    Try(address.validate(Address.addressReads)) match {
      case Success(JsSuccess(a, _)) => if (a.isValid) body else Future.successful(BadRequest)
      case Success(JsError(_)) => Future.successful(BadRequest)
      case Failure(_: JsResultException) => Future.successful(BadRequest)
      case _ => Future.successful(BadRequest)
    }
  }
}
