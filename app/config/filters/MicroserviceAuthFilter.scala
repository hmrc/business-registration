package config.filters

import javax.inject.Inject
import akka.stream.Materializer
import com.typesafe.config.Config
import config.MicroserviceAuthConnector
import play.api.Configuration
import uk.gov.hmrc.play.auth.controllers.AuthParamsControllerConfig
import uk.gov.hmrc.play.auth.microservice.connectors.AuthConnector
import uk.gov.hmrc.play.auth.microservice.filters.AuthorisationFilter

/**
  * Created by jackie on 15/02/17.
  */
class MicroserviceAuthFilter @Inject()(conf: Configuration,
                                       controllerConf: ControllerConfiguration
                                      ) (implicit val mat: Materializer) extends AuthorisationFilter {
  override def authConnector: AuthConnector = MicroserviceAuthConnector
  override def controllerNeedsAuth(controllerName: String): Boolean = {
    controllerConf.paramsForController(controllerName).needsAuth
  }
  override def authParamsConfig: AuthParamsControllerConfig = new AuthParamsControllerConfig() {
    override def controllerConfigs: Config = conf.underlying.atPath("controllers")
  }
}
