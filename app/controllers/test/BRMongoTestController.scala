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

package controllers.test

import javax.inject.{Inject, Singleton}

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Action
import repositories._
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

//class BRMongoTestControllerImp @Inject() (repositories: Repositories) extends BRMongoTestController {
//  val metadataRepository = repositories.metadataRepository
//}

@Singleton
class BRMongoTestController @Inject()(repository: MetadataRepository) extends BaseController {

  def dropMetadataCollection = Action.async {
    implicit request =>
      repository.drop map {
        case true => Ok(Json.parse("""{"message":"Metadata collection dropped successfully"}"""))
        case false => Ok(Json.parse("""{"message":"An error occurred. Metadata collection could not be dropped"}"""))
      }
  }

  def updateCC(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      withJsonBody[JsValue] { cc =>
        val capacity = (cc \ "cc").as[String]
        repository.updateCompletionCapacity(regId, capacity) map(_ => Ok(cc))
      }
  }
}
