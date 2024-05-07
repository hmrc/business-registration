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

package controllers.admin

import models.ErrorResponse
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.MetadataService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AdminController @Inject()(metadataService: MetadataService,
                                controllerComponents: ControllerComponents
                               )  (implicit ec: ExecutionContext) extends BackendController(controllerComponents) {

  def retrieveBusinessRegistration(registrationID: String): Action[AnyContent] = Action.async {
    implicit request =>
      metadataService.retrieveMetadataRecord(registrationID) map {
        case Some(response) => Ok(Json.toJson(response).as[JsObject] ++ metadataService.buildSelfLink(registrationID))
        case None => NotFound(ErrorResponse.MetadataNotFound)
      }
  }

  def removeMetadata(registrationID: String): Action[AnyContent] = Action.async {
    implicit request =>
      metadataService.removeMetadata(registrationID) map (
        if (_) Ok else NotFound
      )
  }
}
