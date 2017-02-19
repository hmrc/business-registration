package config.filters

import javax.inject.Inject
import play.api.Configuration

/**
  * Created by jackie on 15/02/17.
  */
class ControllerConfiguration @Inject()(conf: Configuration) {

  val rootConfig = "controllers"

  def paramsForController(controllerName: String): ControllerParams = {
    val controller = s"$rootConfig.$controllerName"
    ControllerParams(
      needsLogging = conf.getBoolean(s"$controller.needsLogging").getOrElse(true),
      needsAuditing = conf.getBoolean(s"$controller.needsAuditing").getOrElse(true),
      needsAuth = conf.getBoolean(s"$controller.needsAuth").getOrElse(true)
    )
  }

}

case class ControllerParams(needsLogging: Boolean = true, needsAuditing: Boolean = true, needsAuth: Boolean = true)

