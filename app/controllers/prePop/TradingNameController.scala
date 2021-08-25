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

package controllers.prePop

import controllers.helper.AuthControllerHelpers
import javax.inject.{Inject, Singleton}
import models.prepop.{PermissionDenied, TradingName}
import play.api.libs.json.{Format, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import repositories.prepop.TradingNameRepository
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class TradingNameController @Inject()(tradingNameRepository: TradingNameRepository,
                                      val authConnector: AuthConnector,
                                      controllerComponents: ControllerComponents
                                     ) extends BackendController(controllerComponents) with AuthControllerHelpers {

  def getTradingName(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthenticated(
        failure = authenticationResultHandler("getTradingName"),
        success = { internalId =>
          tradingNameRepository.getTradingName(regId, internalId) map (
            _.fold[Result](NoContent)(s => Ok(Json.toJson(s)(TradingName.format)))
            ) recover {
            case _: PermissionDenied => Forbidden
          }
        }
      )
  }

  def upsertTradingName(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      implicit val reads: Format[String] = TradingName.format
      isAuthenticated(
        failure = authenticationResultHandler("getTradingName"),
        success = { internalId =>
          withJsonBody[String] { tradingName =>
            tradingNameRepository.upsertTradingName(regId, internalId, tradingName) map (
              _.fold[Result](InternalServerError)(s => Ok(Json.toJson(s)))
              ) recover {
              case _: PermissionDenied => Forbidden
            }
          }
        }
      )
  }
}
