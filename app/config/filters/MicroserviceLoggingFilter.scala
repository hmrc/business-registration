package config.filters

import javax.inject.Inject

import uk.gov.hmrc.play.filters.MicroserviceFilterSupport
import uk.gov.hmrc.play.http.logging.filters.LoggingFilter

/**
  * Created by jackie on 15/02/17.
  */
class MicroserviceLoggingFilter @Inject()(
                                           controllerConf: ControllerConfiguration
                                         ) extends LoggingFilter with MicroserviceFilterSupport {

  override def controllerNeedsLogging(controllerName: String) =
    controllerConf.paramsForController(controllerName).needsLogging
}
