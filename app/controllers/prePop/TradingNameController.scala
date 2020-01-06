/*
 * Copyright 2020 HM Revenue & Customs
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
import javax.inject.Inject
import models.prepop.{PermissionDenied, TradingName}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, Result}
import repositories.prepop.{TradingNameMongo, TradingNameRepository}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import scala.concurrent.ExecutionContext.Implicits.global

class TradingNameControllerImpl @Inject()(tradingNameMongo: TradingNameMongo,
                                          val authConnector:AuthConnector) extends TradingNameController {

  val tradingNameRepo: TradingNameRepository = tradingNameMongo.repository
}
trait TradingNameController extends BaseController with AuthControllerHelpers {
  val tradingNameRepo: TradingNameRepository

  def getTradingName(regId: String): Action[AnyContent] = Action.async{
    implicit request => isAuthenticated(
      failure = authenticationResultHandler("getTradingName"),
      success = { internalId =>
        tradingNameRepo.getTradingName(regId, internalId) map (
          _.fold[Result](NoContent)(s => Ok(Json.toJson(s)(TradingName.format)))
          ) recover {
          case _: PermissionDenied => Forbidden
        }
      }
    )
  }

  def upsertTradingName(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      implicit val reads = TradingName.format
      isAuthenticated(
        failure = authenticationResultHandler("getTradingName"),
        success = { internalId =>
          withJsonBody[String] { tradingName =>
            tradingNameRepo.upsertTradingName(regId, internalId, tradingName) map (
              _.fold[Result](InternalServerError)(s => Ok(Json.toJson(s)))
              ) recover {
              case _: PermissionDenied => Forbidden
            }
          }
        }
      )
  }
}
