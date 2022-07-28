/*
 * Copyright 2022 HM Revenue & Customs
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

package auth

import play.api.Logging
import play.api.mvc.Result
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.{AuthorisationException, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

sealed trait AuthenticationResult

case object NotLoggedIn extends AuthenticationResult

final case class LoggedIn(id: String) extends AuthenticationResult

trait Authenticated extends AuthorisedFunctions with Logging {

  def isAuthenticated(failure: AuthenticationResult => Future[Result],
                      success: String => Future[Result]
                     )(implicit hc: HeaderCarrier): Future[Result] =

    authorised().retrieve(internalId) {
      case Some(id) =>
        success(id)
      case _ =>
        failure(NotLoggedIn)
    }.recoverWith {
      case _: AuthorisationException =>
        failure(NotLoggedIn)
      case err =>
        logger.error(s"[Authenticated][isAuthenticated] an error occurred with message: ${err.getMessage}")
        throw err
    }

}
