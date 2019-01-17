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

package config

import com.typesafe.config.Config
import javax.inject.Inject
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.{HttpHook, HttpHooks}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http.ws._

trait WSHttp extends
  HttpGet with WSGet with
  HttpPut with WSPut with
  HttpPatch with WSPatch with
  HttpPost with WSPost with
  HttpDelete with WSDelete with
  HttpHooks with AppName

class ServicesConfigImpl @Inject()(val environment: Environment, conf: Configuration ) extends ServicesConfig {
  override protected def mode: Mode = environment.mode
  override protected def runModeConfiguration: Configuration = conf
}

class WSHttpImpl @Inject()(val appNameConfiguration: Configuration,val microserviceAuditConnector: AuditConnector) extends WSHttp with HttpAuditing {
  override def configuration: Option[Config] = Option(appNameConfiguration.underlying)
  override val hooks: Seq[HttpHook] = Seq(AuditingHook)
  override def auditConnector = microserviceAuditConnector
}