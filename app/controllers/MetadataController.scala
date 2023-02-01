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

package controllers

import auth._
import controllers.helper.AuthControllerHelpers
import models._
import play.api.libs.json.{JsNumber, JsObject, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.MetadataMongoRepository
import services.{MetadataService, MetricsService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.InstantJsonUtil

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class MetadataController @Inject()(metadataService: MetadataService,
                                   metricsService: MetricsService,
                                   val resourceConn: MetadataMongoRepository,
                                   val authConnector: AuthConnector,
                                   controllerComponents: ControllerComponents
                                  ) extends BackendController(controllerComponents) with Authorisation with AuthControllerHelpers with InstantJsonUtil {

  def createMetadata: Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      metricsService.createFootprintCounter.inc()
      isAuthenticated(
        failure = authenticationFailureResultHandler("createMetaData"),
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

  def searchMetadata: Action[AnyContent] = Action.async {
    implicit request =>
      isAuthenticated(
        failure = authenticationFailureResultHandler("searchMetaData"),
        success = { internalId =>
          val timer = metricsService.searchMetadataTimer.time()
          metadataService.searchMetadataRecord(internalId) map (
            _.fold(NotFound(ErrorResponse.MetadataNotFound)) { response =>
              timer.stop()
              Ok(Json.toJson(response).as[JsObject] ++ buildSelfLink(response.registrationID))
            }
            )
        })
  }

  def retrieveMetadata(registrationID: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(registrationID)(
        failure = authorisationFailureResultHandler("retrieveMetadata"),
        success = {
          val timer = metricsService.retrieveMetadataTimer.time()
          metadataService.retrieveMetadataRecord(registrationID) map (
            _.fold(NotFound(ErrorResponse.MetadataNotFound)) {
              response =>
                timer.stop()
                Ok(Json.toJson(response).as[JsObject] ++ buildSelfLink(registrationID))
            }
            )
        })
  }

  def removeMetadata(registrationID: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(registrationID)(
        failure = authorisationFailureResultHandler("removeMetadata"),
        success = {
          val timer = metricsService.removeMetadataTimer.time()
          metadataService.removeMetadata(registrationID) map (
            if (_) {
              timer.stop()
              Ok
            } else {
              NotFound
            }
            )
        })
  }

  def updateMetaData(registrationID: String): Action[JsValue] = Action.async[JsValue](parse.json) {
    implicit request =>
      isAuthorised(registrationID)(
        failure = authorisationFailureResultHandler("updateMetadata"),
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

  def updateLastSignedIn(registrationId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      isAuthorised(registrationId)(
        failure = authorisationFailureResultHandler("updateLastSignedIn"),
        success = {
          implicit val instantReads = flexibleInstantReads
          withJsonBody[Instant] { instant =>
            metadataService.updateLastSignedIn(registrationId, instant) map { updatedDT => Ok(JsNumber(updatedDT.toEpochMilli)) }
          }
        })
  }

  private[controllers] def buildSelfLink(registrationId: String): JsObject = {
    Json.obj("links" -> Links(Some(controllers.routes.MetadataController.retrieveMetadata(registrationId).url)))
  }
}
