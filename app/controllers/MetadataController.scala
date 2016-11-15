/*
 * Copyright 2016 HM Revenue & Customs
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

package controllers

import auth._
import connectors.AuthConnector
import models.{ErrorResponse, Metadata, MetadataRequest, MetadataResponse}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Action
import play.api.Logger
import services.MetadataService
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object MetadataController extends MetadataController {
  val metadataService = MetadataService
  val resourceConn = MetadataService.metadataRepository
  val auth = AuthConnector
}

trait MetadataController extends BaseController with Authenticated with Authorisation[String] {

  val metadataService: MetadataService

  def createMetadata: Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      authenticated {
        case NotLoggedIn => Future.successful(Forbidden)
        case LoggedIn(context) =>
          withJsonBody[MetadataRequest] {
            request => {
              metadataService.createMetadataRecord(context.oid, request.language) map {
                r => Created(Json.toJson(r))
              }
            }
          }
        }
  }

  def searchMetadata = Action.async {
    implicit request =>
      authenticated {
        case NotLoggedIn => Future.successful(Forbidden)
        case LoggedIn(context) => {
          metadataService.searchMetadataRecord(context.oid) map {
            case Some(response) => Ok(Json.toJson(response))
            case None => NotFound(ErrorResponse.MetadataNotFound)
          }
        }
      }
  }

  def retrieveMetadata(registrationID: String) = Action.async {
    implicit request =>
      authorisedFor(registrationID) { _ =>
        metadataService.retrieveMetadataRecord(registrationID) map {
          case Some(response) =>
            Ok(
              Json.toJson(response)
//                .as[JsObject] ++
//              Json.obj("links" -> Json.obj(
//                  "xxx" -> "yyy"
//                )
//              )
            )
          case None => NotFound(ErrorResponse.MetadataNotFound)
        }
      }
  }

  def updateMetaData(registrationID : String) : Action[JsValue] = Action.async[JsValue](parse.json) {
    implicit request =>
      authorisedFor(registrationID) { _ =>
        withJsonBody[MetadataResponse] {
          metaData =>
            metadataService.updateMetaDataRecord(registrationID, metaData) map {
              metaDataResp => Ok(Json.toJson(metaDataResp))
            }
        }
      }
  }
}
