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

package controllers

import javax.inject.{Singleton, Inject}

import auth._
import connectors.AuthConnector
import models._
import org.joda.time.DateTime
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Action
import play.api.Logger
import repositories.MetadataMongoRepository
import services.{MetadataService, MetricsService}
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

//class MetadataControllerImp @Inject() (metadata: MetadataService, authConnector: AuthConnector, metrics: MetricsService)
//  extends MetadataController {
//  val metadataService = metadata
//  val resourceConn = metadataService.metadataRepository
//  val auth = authConnector
//  val metricsService: MetricsService = metrics
//}

@Singleton
class MetadataController @Inject() (metadataService: MetadataService, authConnector: AuthConnector, metricsService: MetricsService, metadataRepo: MetadataMongoRepository)
  extends BaseController with Authenticated with Authorisation[String] {

  val resourceConn = metadataRepo
  val auth = authConnector

  def createMetadata: Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      metricsService.createFootprintCounter.inc()
      authenticated {
        case NotLoggedIn => Future.successful(Forbidden)
        case LoggedIn(context) =>
          val timer = metricsService.createMetadataTimer.time()
          withJsonBody[MetadataRequest]
            {
            request => {
              metricsService.createFootprintCounter.inc()
              metadataService.createMetadataRecord(context.ids.internalId, request.language) map {
                response => {
                  timer.stop()
                  Created(Json.toJson(response).as[JsObject] ++ buildSelfLink(response.registrationID))
                }
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
        val timer = metricsService.removeMetadataTimer.time()
        metadataService.removeMetadata(registrationID) map {
          case true => timer.stop()
                       Ok
          case false => NotFound
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

  def updateLastSignedIn(registrationId: String) = Action.async(parse.json) {
    implicit request =>
      authorisedFor(registrationId) { _ =>
        withJsonBody[DateTime] { dT =>
          metadataService.updateLastSignedIn(registrationId, dT) map { updatedDT => Ok(Json.toJson(updatedDT))}
        }
      }
  }

  private[controllers] def buildSelfLink(registrationId: String): JsObject = {
    Json.obj("links" -> Links(Some(controllers.routes.MetadataController.retrieveMetadata(registrationId).url)))
  }
}
