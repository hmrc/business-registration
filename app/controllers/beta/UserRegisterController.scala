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

package controllers.beta

import javax.inject.Inject

import models.WhiteListDetailsSubmit
import play.api.libs.json.JsValue
import play.api.mvc.Action
import services.UserRegisterService
import uk.gov.hmrc.play.microservice.controller.BaseController

class UserRegisterControllerImp @Inject() (userRegisterServ: UserRegisterService) extends UserRegisterController {
  val userRegisterService = userRegisterServ
}

trait UserRegisterController extends BaseController {

  val userRegisterService : UserRegisterService

  def createUserRecord : Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[WhiteListDetailsSubmit] {
      userDetails =>
        userRegisterService.createRegistration(userDetails)
    }
  }
}
