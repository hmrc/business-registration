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

package models

import play.api.libs.json.{JsValue, Json}

case class ErrorResponse(statusCode: String, message: String)

object ErrorResponse{
  implicit val formats = Json.format[ErrorResponse]

  def toJson(res: ErrorResponse): JsValue = {
    Json.toJson(res)
  }

  //TODO: Should we use the error response in the bootstrap
  lazy val MetadataNotFound: JsValue = toJson(ErrorResponse("404", "Could not find metadata record"))

  lazy val UserNotFound : JsValue = toJson(ErrorResponse("404","Could not find user record"))
}
