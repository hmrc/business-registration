package config

import javax.inject.Inject

import play.api.{Application, Logger}

/**
  * Created by jackie on 15/02/17.
  */
trait AppStartup {

  protected def app: Application
  protected def graphiteConfig: GraphiteConfig
  protected def appName: String

}

class DefaultAppStartup @Inject()(
                                   val app: Application
                                 ) extends AppStartup {

  override lazy val graphiteConfig: GraphiteConfig = new GraphiteConfig(app)

  override lazy val appName: String = app.configuration.getString("appName").getOrElse("APP NAME NOT SET")

  Logger.info(s"Starting microservice : $appName : in mode : ${app.mode}")
  if(graphiteConfig.enabled) graphiteConfig.startGraphite()

}