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
import models._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Action
import play.api.Logger
import services.{MetadataService, MetricsService}
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object MetadataController extends MetadataController {
  val metadataService = MetadataService
  val resourceConn = MetadataService.metadataRepository
  val auth = AuthConnector
  override val metricsService: MetricsService = MetricsService
}

trait MetadataController extends BaseController with Authenticated with Authorisation[String] {

  val metadataService: MetadataService
  val metricsService: MetricsService

  def createMetadata: Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      authenticated {
        case NotLoggedIn => Future.successful(Forbidden)
        case LoggedIn(context) =>
          val timer = metricsService.createMetadataTimer.time()
          withJsonBody[MetadataRequest]
            {
            request => {
              metadataService.createMetadataRecord(context.ids.internalId, request.language) map {
                response => timer.stop()
                            Created(Json.toJson(response).as[JsObject] ++ buildSelfLink(response.registrationID))
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
          val timer = metricsService.searchMetadataTimer.time()
          metadataService.searchMetadataRecord(context.ids.internalId) map {
            case Some(response) => timer.stop()
                                   Ok(Json.toJson(response).as[JsObject] ++ buildSelfLink(response.registrationID))
            case None => NotFound(ErrorResponse.MetadataNotFound)
          }
        }
      }
  }

  def retrieveMetadata(registrationID: String) = Action.async {
    implicit request =>
      authorisedFor(registrationID) { _ =>
        val timer = metricsService.retrieveMetadataTimer.time()
        metadataService.retrieveMetadataRecord(registrationID) map {
          case Some(response) => timer.stop()
                                 Ok(Json.toJson(response).as[JsObject] ++ buildSelfLink(registrationID))
          case None => NotFound(ErrorResponse.MetadataNotFound)
        }
      }
  }

  def removeMetadata(registrationID: String) = Action.async {
    implicit request =>
      authorisedFor(registrationID) { _ =>
        val timer = metricsService.removeMetadataTimer.time()
        metadataService.removeMetadata(registrationID) map {
          case true => timer.stop()
                       Ok
          case false => NotFound
        }
      }
  }

  def updateMetaData(registrationID : String) : Action[JsValue] = Action.async[JsValue](parse.json) {
    implicit request =>
      authorisedFor(registrationID) { _ =>
        withJsonBody[MetadataResponse] {
          metaData =>
            val timer = metricsService.updateMetadataTimer.time()
            metadataService.updateMetaDataRecord(registrationID, metaData) map {
              response => timer.stop()
                          Ok(Json.toJson(response).as[JsObject] ++ buildSelfLink(registrationID))
            }
        }
      }
  }

  private[controllers] def buildSelfLink(registrationId: String): JsObject = {
    Json.obj("links" -> Links(Some(controllers.routes.MetadataController.retrieveMetadata(registrationId).url)))
  }
}
