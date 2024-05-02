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

import controllers.helper.AuthControllerHelpers
import models.prepop.{ContactDetails, PermissionDenied}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import repositories.MetadataMongoRepository
import repositories.prepop.ContactDetailsRepository
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ContactDetailsController @Inject()(val resourceConn: MetadataMongoRepository,
                                         contactDetailsRepository: ContactDetailsRepository,
                                         val authConnector: AuthConnector,
                                         controllerComponents: ControllerComponents
                                        ) (implicit ec: ExecutionContext) extends BackendController(controllerComponents) with AuthControllerHelpers {

  def getContactDetails(registrationID: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthenticated(
        failure = authenticationFailureResultHandler("getContactDetails"),
        success = { internalId =>
          contactDetailsRepository.getContactDetails(registrationID, internalId) map (
            _.fold[Result](NotFound)(s => Ok(Json.toJson(s)))
            ) recover {
            case _: PermissionDenied => Forbidden
          }
        }
      )
  }

  def insertUpdateContactDetails(registrationID: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      isAuthenticated(
        failure = authenticationFailureResultHandler("insertUpdateContactDetails"),
        success = { internalId =>
          withJsonBody[ContactDetails] { js =>
            contactDetailsRepository.upsertContactDetails(registrationID, internalId, js) map (
              _.fold(NotFound)(x => Ok)
              ) recover {
              case _: PermissionDenied => Forbidden
            }
          }
        }
      )
  }
}
