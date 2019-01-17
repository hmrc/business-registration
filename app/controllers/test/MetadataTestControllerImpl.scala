/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.Inject
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Action
import repositories._
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import scala.concurrent.ExecutionContext.Implicits.global

class MetadataTestControllerImpl @Inject()(metaDataRepo: MetadataMongo) extends BRMongoTestController {
  val repo = metaDataRepo.repository
}

trait BRMongoTestController extends BaseController {
  val repo: MetadataRepository

  def dropMetadataCollection = Action.async {
    implicit request =>
      repo.drop map {
        case true => Ok(Json.parse("""{"message":"Metadata collection dropped successfully"}"""))
        case false => Ok(Json.parse("""{"message":"An error occurred. Metadata collection could not be dropped"}"""))
      }
  }

  def updateCompletionCapacity(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      withJsonBody[JsValue] { cc =>
        val capacity = (cc \ "completionCapacity").as[String]
        repo.updateCompletionCapacity(regId, capacity) map(_ => Ok(cc))
      }
  }
}
