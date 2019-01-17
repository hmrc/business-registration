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

package auth

import play.api.Logger
import play.api.mvc.Result
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.{AuthorisationException, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

sealed trait AuthenticationResult
case object NotLoggedIn extends AuthenticationResult
final case class LoggedIn(id: String) extends AuthenticationResult

trait Authenticated extends AuthorisedFunctions {

  def isAuthenticated(failure: AuthenticationResult => Future[Result], success: String => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    authorised().retrieve(internalId)(id => mapToAuthResult(id) match {
      case LoggedIn(intId)  => success(intId)
      case result           => failure(result)
    }).recoverWith {
      case _: AuthorisationException => failure(NotLoggedIn)
      case err => Logger.error(s"[Authenticated][isAuthenticated] an error occured with message: ${err.getMessage()}")
        throw err
    }
  }

  private def mapToAuthResult(internalId: Option[String]) : AuthenticationResult = {
    internalId.fold[AuthenticationResult](NotLoggedIn)(LoggedIn)
  }
}
