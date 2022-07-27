/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.test

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories._
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class MetadataTestController @Inject()(metadataMongoRepository: MetadataMongoRepository,
                                       controllerComponents: ControllerComponents
                                      ) extends BackendController(controllerComponents) {


  def dropMetadataCollection: Action[AnyContent] = Action.async {
    implicit request =>
      metadataMongoRepository.drop.map {
        case true => Ok(Json.parse("""{"message":"Metadata collection dropped successfully"}"""))
        case false => Ok(Json.parse("""{"message":"An error occurred. Metadata collection could not be dropped"}"""))
      }
  }

  def updateCompletionCapacity(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      withJsonBody[JsValue] { cc =>
        val capacity = (cc \ "completionCapacity").as[String]
        metadataMongoRepository.updateCompletionCapacity(regId, capacity) map (_ => Ok(cc))
      }
  }
}
