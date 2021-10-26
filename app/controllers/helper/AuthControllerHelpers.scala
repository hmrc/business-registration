/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.helper

import auth._
import play.api.Logging
import play.api.mvc.Result
import play.api.mvc.Results._

import scala.concurrent.Future

trait AuthControllerHelpers extends Authenticated with Logging {
  def authenticationResultHandler(methodName: String)(authResult: AuthenticationResult): Future[Result] = authResult match {
    case NotLoggedIn => logger.info(s"[Authentication] [$methodName] User not logged in")
      Future.successful(Forbidden)
  }

  def authorisationResultHandler(methodName: String)(regId: String, authResult: AuthorisationResult): Future[Result] = authResult match {
    case NotLoggedInOrAuthorised =>
      logger.info(s"[Authorisation] [$methodName] User not logged in")
      Future.successful(Forbidden)
    case NotAuthorised(_) =>
      logger.info(s"[Authorisation] [$methodName] User logged in but not authorised for resource $regId")
      Future.successful(Forbidden)
    case AuthResourceNotFound(_) =>
      logger.info(s"[Authorisation] [$methodName] Could not match an Auth resource to registration id $regId")
      Future.successful(NotFound)
  }

}
