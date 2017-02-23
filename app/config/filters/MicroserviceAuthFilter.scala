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

package config.filters

import javax.inject.{Singleton, Inject}
import akka.stream.Materializer
import com.typesafe.config.Config
import play.api.{Application, Configuration}
import uk.gov.hmrc.play.auth.controllers.AuthParamsControllerConfig
import uk.gov.hmrc.play.auth.microservice.connectors.AuthConnector
import uk.gov.hmrc.play.auth.microservice.filters.AuthorisationFilter
import uk.gov.hmrc.play.config.ServicesConfig

class MicroserviceAuthFilter @Inject()(app: Application, conf: Configuration, controllerConf: ControllerConfiguration)
                                      (implicit val mat: Materializer) extends AuthorisationFilter {
  override def authConnector: AuthConnector = new MicroserviceAuthConnector(app)
  override def controllerNeedsAuth(controllerName: String): Boolean = {
    controllerConf.paramsForController(controllerName).needsAuth
  }
  override def authParamsConfig: AuthParamsControllerConfig = new AuthParamsControllerConfig() {
    override def controllerConfigs: Config = conf.underlying.atPath("controllers")
  }
}

@Singleton
class MicroserviceAuthConnector @Inject()(val app: Application) extends AuthConnector with ServicesConfig {
  override val authBaseUrl = baseUrl("auth")
}
