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

package controllers.prePop

import javax.inject.{Inject, Singleton}

import auth.{Authenticated, LoggedIn, NotLoggedIn}
import connectors.AuthConnector
import models.prepop.{ContactDetails, PermissionDenied}
import play.api.libs.json.Json
import play.api.mvc.Action
import repositories.MetadataMongo
import repositories.prepop.{ContactDetailsMongo, ContactDetailsRepository}
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.Future
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

@Singleton
class ContactDetailsControllerImpl @Inject()(val authConnector: AuthConnector,
                                             val repo: MetadataMongo, val contactDetailsrepo: ContactDetailsMongo
                                            ) extends ContactDetailsController{

  val resourceConn = repo.repository
  val cdRepository = contactDetailsrepo.repository
}

trait ContactDetailsController extends BaseController with Authenticated {

  val cdRepository: ContactDetailsRepository
  def getContactDetails(registrationID : String)  = Action.async {
    implicit request =>
      authenticated {
         case NotLoggedIn => Future.successful(Forbidden)
         case LoggedIn(context) =>
           cdRepository.getContactDetails(registrationID, context.ids.internalId) map{
              case Some(s:ContactDetails) => Ok(Json.toJson(s))
              case _ => NotFound
        }recover{case p:PermissionDenied => Forbidden}
      }
  }

  def insertUpdateContactDetails(registrationID: String) = Action.async(parse.json) {
    implicit request =>
      authenticated {
        case NotLoggedIn => Future.successful(Forbidden)
        case LoggedIn(context) => withJsonBody[ContactDetails] { js =>
           cdRepository.upsertContactDetails(registrationID, context.ids.internalId, js) map {
             case Some(s:ContactDetails) => Ok
             case _ => NotFound
           }recover{ case p:PermissionDenied => Forbidden}
          }
        }
      }
  }




