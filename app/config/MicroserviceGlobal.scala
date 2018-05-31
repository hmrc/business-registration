/*
 * Copyright 2018 HM Revenue & Customs
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
import net.ceedubs.ficus.Ficus._
import play.api.{Application, Configuration, Logger, Play}
import reactivemongo.api.indexes.Index
import repositories.prepop.{ContactDetailsMongo, TradingNameMongo}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.auth.controllers.AuthParamsControllerConfig
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.microservice.bootstrap.DefaultMicroserviceGlobal
import uk.gov.hmrc.play.microservice.filters.{AuditFilter, LoggingFilter, MicroserviceFilterSupport}

import scala.concurrent.ExecutionContext.Implicits.global
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

object MicroserviceGlobal extends MicroserviceGlobal

abstract class MicroserviceGlobal extends DefaultMicroserviceGlobal with RunMode {
  override val auditConnector: AuditConnector = MicroserviceAuditConnector

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"microservice.metrics")

  override val loggingFilter: LoggingFilter = MicroserviceLoggingFilter

  override val microserviceAuditFilter: AuditFilter = MicroserviceAuditFilter

  override val authFilter = None

  override def onStart(app : play.api.Application) : scala.Unit = {
    super.onStart(app)
    val repoIndexEnsurer = RepositoryIndexEnsurer(app)
    repoIndexEnsurer.ensureIndexes()
  }
}

case class RepositoryIndexEnsurer(app: Application) {
  def ensureIndexes(): Future[Unit] = {
    ensureIndexes(app.injector.instanceOf[ContactDetailsMongo].repository)
    ensureIndexes(app.injector.instanceOf[TradingNameMongo].repository)
  }

  def deleteIndexes(index: String): Future[Unit] = {
    val repo = app.injector.instanceOf[ContactDetailsMongo].repository
    repo.collection.indexesManager.list().map { l =>
      l.filter(i => i.eventualName == index) map {
        i => repo.collection.indexesManager.drop(i.eventualName)
      }
    }
  }

  private def ensureIndexes(repo: ReactiveRepository[_, _]): Future[Unit] = {
    for{
      _ <- repo.ensureIndexes
      _ <- fetchLatestIndexes(repo)
    } yield ()
  }

  private def fetchLatestIndexes(repo: ReactiveRepository[_, _]): Future[List[Index]] = {
    val collectionName = repo.collection.name
    repo.collection.indexesManager.list() map { indexes =>
      indexes.map{ index =>
        val indexOptions = index.options.elements.toString()
        Logger.info(s"[EnsuringIndexes] Collection : $collectionName \n" +
          s"Index : ${index.eventualName} \n" +
          s"""keys : ${index.key match {
            case Seq(s @ _*) => s"$s\n"
            case Nil => "None\n"}}""" +
          s"Is Unique? : ${index.unique}\n" +
          s"In Background? : ${index.background}\n" +
          s"Is sparse? : ${index.sparse}\n" +
          s"version : ${index.version}\n" +
          s"partialFilter : ${index.partialFilter.map(_.values)}\n" +
          s"Options : $indexOptions")
        index
      }
    }
  }
}
