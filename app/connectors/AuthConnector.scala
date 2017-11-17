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

package connectors

import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import config.{MicroserviceAppConfig, WSHttp}
import models.{Authority, UserIds}
import play.api.http.Status._
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier, HttpResponse}

import scala.concurrent.Future
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

@Singleton
class AuthConnectorImpl @Inject()(config: MicroserviceAppConfig) extends AuthConnector with RawResponseReads {

  val http: CoreGet = WSHttp

  lazy val serviceUrl = config.authUrl

  def authorityUri = "auth/authority"

  def getCurrentAuthority()(implicit headerCarrier: HeaderCarrier): Future[Option[Authority]] = {
    val getUrl = s"""$serviceUrl/$authorityUri"""
    http.GET[HttpResponse](getUrl) flatMap { response =>
      response.status match {
        case OK => {
          val uri         = (response.json \ "uri").as[String]
          val userDetails = (response.json \ "userDetailsLink").as[String]
          val idsLink     = (response.json \ "ids").as[String]

          http.GET[HttpResponse](s"$serviceUrl$idsLink") map { response =>
            val ids = response.json.as[UserIds]
            Some(Authority(uri, userDetails, ids))
          }
        }
        case _ => Future.successful(None)
      }
    }
  }
}

@ImplementedBy(classOf[AuthConnectorImpl])
trait AuthConnector {
  def getCurrentAuthority()(implicit headerCarrier: HeaderCarrier): Future[Option[Authority]]
}
