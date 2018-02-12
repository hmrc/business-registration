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

package auth

import play.api.Logger
import play.api.mvc.Result
import uk.gov.hmrc.auth.core.retrieve.Retrievals.internalId
import uk.gov.hmrc.auth.core.{AuthorisationException, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

sealed trait AuthorisationResult
case object NotLoggedInOrAuthorised extends AuthorisationResult
final case class NotAuthorised(inId: String) extends AuthorisationResult
final case class Authorised(intId: String) extends AuthorisationResult
final case class AuthResourceNotFound(intId: String) extends AuthorisationResult

trait Authorisation extends AuthorisedFunctions {
  val resourceConn: AuthorisationResource

  def isAuthorised(id: String)(failure: (String, AuthorisationResult) => Future[Result], success: => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    authorised().retrieve(internalId)( intId => resourceConn.getInternalId(id) flatMap (
      resource => mapToAuthResult(intId, resource) match {
        case Authorised(_)  => success
        case result         => failure(id, result)
      })).recoverWith {
        case _: AuthorisationException => failure(id, NotLoggedInOrAuthorised)
        case err => Logger.error(s"[Authorisation][isAuthorised] an error occurred for regId: $id with message: ${err.getMessage()}")
          throw err
    }
  }

  private[auth] def mapToAuthResult(internalId: Option[String], resource: Option[String]) : AuthorisationResult = {
    internalId match {
      case None => NotLoggedInOrAuthorised
      case Some(intId) =>
        resource match {
          case None => AuthResourceNotFound(intId)
          case Some(`intId`)  => Authorised (intId)
          case _ => NotAuthorised (intId)
        }
    }
  }
}
