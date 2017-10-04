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

package config

import play.api.{Application, Configuration, Logger, Play}
import uk.gov.hmrc.play.audit.filters.AuditFilter
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.auth.controllers.AuthParamsControllerConfig
import uk.gov.hmrc.play.auth.microservice.filters.AuthorisationFilter
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.filters.MicroserviceFilterSupport
import uk.gov.hmrc.play.http.logging.filters.LoggingFilter
import uk.gov.hmrc.play.microservice.bootstrap.DefaultMicroserviceGlobal
import com.typesafe.config.Config
import uk.gov.hmrc.play.auth.microservice.connectors.AuthConnector
import net.ceedubs.ficus.Ficus._
import reactivemongo.api.indexes.Index
import repositories.prepop.ContactDetailsMongo
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.Future

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs: Config = Play.current.configuration.underlying.as[Config]("controllers")
}

object AuthParamsControllerConfiguration extends AuthParamsControllerConfig {
  lazy val controllerConfigs: Config = ControllerConfiguration.controllerConfigs
}

object MicroserviceAuditFilter extends AuditFilter with AppName with MicroserviceFilterSupport {
  override val auditConnector: AuditConnector = MicroserviceAuditConnector
  override def controllerNeedsAuditing(controllerName: String): Boolean = ControllerConfiguration.paramsForController(controllerName).needsAuditing
}

object MicroserviceLoggingFilter extends LoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String): Boolean = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object MicroserviceAuthFilter extends AuthorisationFilter with MicroserviceFilterSupport {
  override lazy val authParamsConfig: AuthParamsControllerConfiguration.type = AuthParamsControllerConfiguration
  override lazy val authConnector: AuthConnector = MicroserviceAuthConnector
  override def controllerNeedsAuth(controllerName: String): Boolean = ControllerConfiguration.paramsForController(controllerName).needsAuth
}

object MicroserviceGlobal extends MicroserviceGlobal

abstract class MicroserviceGlobal extends DefaultMicroserviceGlobal with RunMode {
  override val auditConnector: AuditConnector = MicroserviceAuditConnector

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"microservice.metrics")

  override val loggingFilter: LoggingFilter = MicroserviceLoggingFilter

  override val microserviceAuditFilter: AuditFilter = MicroserviceAuditFilter

  override val authFilter = Some(MicroserviceAuthFilter)

  override def onStart(app : play.api.Application) : scala.Unit = {
    super.onStart(app)

    val repoIndexEnsurer = RepositoryIndexEnsurer(app)
    repoIndexEnsurer.ensureIndexes()
    repoIndexEnsurer.deleteIndexes("uniqueIntID")
  }
}

case class RepositoryIndexEnsurer(app: Application) {
  import scala.concurrent.ExecutionContext.Implicits.global

  def ensureIndexes(): Unit = {
    ensureIndexes(app.injector.instanceOf[ContactDetailsMongo].repository)
  }

  def deleteIndexes(index: String) : Unit = {
    val repo = app.injector.instanceOf[ContactDetailsMongo].repository
    repo.collection.indexesManager.list().map { l =>
      l.filter(i => i.eventualName == index) map {
        i => repo.collection.indexesManager.drop(i.eventualName)
      }
    }
  }

  private def ensureIndexes(repo: ReactiveRepository[_, _]): Unit = {
    for{
      _ <- repo.ensureIndexes
      _ <- fetchLatestIndexes(repo)
    } yield ()
  }

  private def fetchLatestIndexes(repo: ReactiveRepository[_, _]): Future[List[Index]] = {
    val collectionName = repo.collection.name
    repo.collection.indexesManager.list() map { indexes =>
      indexes.map{ index =>
        val indexName = index.eventualName
        val indexOptions = index.options.elements.toString()
        Logger.info(s"[EnsuringIndexes] collection : $collectionName - index : $indexName - options : $indexOptions")
        index
      }
    }
  }
}
