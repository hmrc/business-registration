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

package auth

import models.Authority
import play.api.mvc.Result
import play.api.Logger
import connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import play.api.mvc.Results._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

sealed trait AuthorisationResult
case object NotLoggedInOrAuthorised extends AuthorisationResult
final case class NotAuthorised(authContext: Authority) extends AuthorisationResult
final case class Authorised(authContext: Authority) extends AuthorisationResult
final case class AuthResourceNotFound(authContext: Authority) extends AuthorisationResult

trait Authorisation[I] {

  val authConnector: AuthConnector
  val resourceConn : AuthorisationResource[I]

  def authorised(id:I)(f: => AuthorisationResult => Future[Result])(implicit hc: HeaderCarrier) = {
    for {
      authority <- authConnector.getCurrentAuthority()
      resource <- resourceConn.getInternalId(id)
      result <- f(mapToAuthResult(authority, resource))
    } yield {
      result
    }
  }

  def authorisedFor(registrationId: I)(f: Authority => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    (for {
        authority <- authConnector.getCurrentAuthority()
        resource <- resourceConn.getInternalId(registrationId)
      } yield {
        mapToAuthResult(authority, resource)
      }
    ) flatMap {
    case Authorised(a) => f(a)
    case NotLoggedInOrAuthorised =>
      Logger.info(s"[Authorisation] [authorisedFor] User not logged in")
      Future.successful(Forbidden)
    case NotAuthorised(_) =>
      Logger.info(s"[Authorisation] [authorisedFor] User logged in but not authorised for resource $registrationId")
      Future.successful(Forbidden)
    case AuthResourceNotFound(_) =>
      Logger.info(s"[Authorisation] [authorisedFor] Could not match an Auth resource to registration id $registrationId")
      Future.successful(NotFound)
    }
  }

  private def mapToAuthResult(authContext: Option[Authority], resource: Option[(I,String)] ) : AuthorisationResult = {
    authContext match {
      case None => NotLoggedInOrAuthorised
      case Some(context) => {
        resource match {
          case None => AuthResourceNotFound(context)
          case Some((_, context.ids.internalId))  => Authorised (context)
          case Some((_, _)) => NotAuthorised (context)
        }
      }
    }
  }
}
