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

package controllers.prePop

import javax.inject.Inject

import auth.Authenticated
import config.AuthClientConnector
import controllers.helper.AuthControllerHelpers
import models.prepop.{ContactDetails, PermissionDenied}
import play.api.libs.json.Json
import play.api.mvc.{Action, Result}
import repositories.MetadataMongo
import repositories.prepop.{ContactDetailsMongo, ContactDetailsRepository}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.microservice.controller.BaseController

class ContactDetailsControllerImpl @Inject()(val repo: MetadataMongo,
                                             val contactDetailsrepo: ContactDetailsMongo
                                            ) extends ContactDetailsController {

  val authConnector = AuthClientConnector
  val resourceConn = repo.repository
  val cdRepository = contactDetailsrepo.repository
}

trait ContactDetailsController extends BaseController with AuthControllerHelpers {

  val cdRepository: ContactDetailsRepository

  def getContactDetails(registrationID : String)  = Action.async {
    implicit request =>
      isAuthenticated(
        failure = authenticationResultHandler("getContactDetails"),
        success = { internalId =>
        cdRepository.getContactDetails(registrationID, internalId) map (
            _.fold[Result](NotFound)(s => Ok(Json.toJson(s)))
          ) recover {
            case _: PermissionDenied => Forbidden
          }
        }
      )
  }

  def insertUpdateContactDetails(registrationID: String) = Action.async(parse.json) {
    implicit request =>
      isAuthenticated(
        failure = authenticationResultHandler("insertUpdateContactDetails"),
        success = { internalId =>
        withJsonBody[ContactDetails] { js =>
          cdRepository.upsertContactDetails(registrationID, internalId, js) map (
            _.fold(NotFound)(x => Ok)
          ) recover {
            case _: PermissionDenied => Forbidden
          }
        }
      })
    }
  }
