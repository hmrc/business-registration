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

package controllers

import auth._
import controllers.helper.AuthControllerHelpers
import javax.inject.Inject
import models._
import org.joda.time.DateTime
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Action
import repositories.MetadataMongo
import services.{MetadataService, MetricsService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import scala.concurrent.ExecutionContext.Implicits.global

class MetadataControllerImpl @Inject()(val metadataService: MetadataService,
                                       val metricsService: MetricsService,
                                       metadataRepo: MetadataMongo,
                                       val authConnector:AuthConnector)
  extends MetadataController {
  val resourceConn = metadataRepo.repository
}

trait MetadataController extends BaseController with Authorisation with AuthControllerHelpers {

  val metadataService: MetadataService
  val metricsService: MetricsService
  val authConnector: AuthConnector

  def createMetadata: Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      metricsService.createFootprintCounter.inc()
      isAuthenticated(
        failure = authenticationResultHandler("createMetaData"),
        success = { internalId =>
        val timer = metricsService.createMetadataTimer.time()
        withJsonBody[MetadataRequest] {
          req => {
            metricsService.createFootprintCounter.inc()
            metadataService.createMetadataRecord(internalId, req.language) map {
              response => {
                timer.stop()
                Created(Json.toJson(response).as[JsObject] ++ buildSelfLink(response.registrationID))
              }
            }
          }
        }
      })
  }

  def searchMetadata = Action.async {
    implicit request =>
      isAuthenticated(
        failure = authenticationResultHandler("searchMetaData"),
        success = { internalId =>
          val timer = metricsService.searchMetadataTimer.time()
          metadataService.searchMetadataRecord(internalId) map (
            _.fold(NotFound(ErrorResponse.MetadataNotFound)){ response =>
              timer.stop()
              Ok(Json.toJson(response).as[JsObject] ++ buildSelfLink(response.registrationID))
            }
          )
      })
  }

  def retrieveMetadata(registrationID: String) = Action.async {
    implicit request =>
      isAuthorised(registrationID)(
        failure = authorisationResultHandler("retrieveMetadata"),
        success = {
          val timer = metricsService.retrieveMetadataTimer.time()
          metadataService.retrieveMetadataRecord(registrationID) map (
            _.fold(NotFound(ErrorResponse.MetadataNotFound)) {
              response => timer.stop()
                Ok(Json.toJson(response).as[JsObject] ++ buildSelfLink(registrationID))
            }
          )
      })
  }

  def removeMetadata(registrationID: String) = Action.async {
    implicit request =>
      isAuthorised(registrationID)(
        failure = authorisationResultHandler("removeMetadata"),
        success = {
          val timer = metricsService.removeMetadataTimer.time()
          metadataService.removeMetadata(registrationID) map (
            if (_){ timer.stop()
              Ok
            } else {
              NotFound
            }
          )
      })
  }

  def updateMetaData(registrationID : String) : Action[JsValue] = Action.async[JsValue](parse.json) {
    implicit request =>
      isAuthorised(registrationID)(
        failure = authorisationResultHandler("updateMetadata"),
        success = {
          withJsonBody[MetadataResponse] {
            metaData =>
              val timer = metricsService.updateMetadataTimer.time()
              metadataService.updateMetaDataRecord(registrationID, metaData) map {
                response =>
                  timer.stop()
                  Ok(Json.toJson(response).as[JsObject] ++ buildSelfLink(registrationID))
              }
      }
    })
  }

  def updateLastSignedIn(registrationId: String) = Action.async(parse.json) {
    implicit request =>
      isAuthorised(registrationId)(
        failure = authorisationResultHandler("updateLastSignedIn"),
        success = {
          withJsonBody[DateTime] { dT =>
            metadataService.updateLastSignedIn(registrationId, dT) map { updatedDT => Ok(Json.toJson(updatedDT)) }
          }
      })
  }

  private[controllers] def buildSelfLink(registrationId: String): JsObject = {
    Json.obj("links" -> Links(Some(controllers.routes.MetadataController.retrieveMetadata(registrationId).url)))
  }
}
