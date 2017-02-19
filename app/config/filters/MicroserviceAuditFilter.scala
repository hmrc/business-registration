package config.filters

import javax.inject.Inject

import akka.stream.Materializer
import play.api.Configuration
import uk.gov.hmrc.play.audit.filters.AuditFilter
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.RunMode

/**
  * Created by jackie on 15/02/17.
  */
class MicroserviceAuditFilter @Inject()(conf: Configuration,
                                        controllerConf: ControllerConfiguration
                                       )(implicit val mat: Materializer) extends AuditFilter {

  override lazy val appName = conf.getString("appName").getOrElse("APP NAME NOT SET")

  override def auditConnector: AuditConnector = MicroserviceAuditConnector

  override def controllerNeedsAuditing(controllerName: String): Boolean = {
    controllerConf.paramsForController(controllerName).needsAuditing
  }
}


object MicroserviceAuditConnector extends AuditConnector with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig(s"$env.auditing")
}
