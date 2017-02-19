package config

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit._

import com.codahale.metrics.{MetricFilter, SharedMetricRegistries}
import com.codahale.metrics.graphite.{Graphite, GraphiteReporter}
import play.api.{Application, Configuration, Logger, Mode}

/**
  * Created by jackie on 15/02/17.
  */
class GraphiteConfig(app: Application) {

  private lazy val env = {
    if (app.mode.equals(Mode.Test)) {"Test"}
    else {app.configuration.getString("run.mode").getOrElse("Dev")}
  }

  private def microserviceMetricsConfig: Option[Configuration] = app.configuration.getConfig(s"$env.microservice.metrics")

  def enabled: Boolean = metricsPluginEnabled && graphitePublisherEnabled

  private def metricsPluginEnabled: Boolean =  app.configuration.getBoolean("metrics.enabled").getOrElse(false)

  private def graphitePublisherEnabled: Boolean =  microserviceMetricsConfig.flatMap(_.getBoolean("graphite.enabled")).getOrElse(false)

  private def registryName = app.configuration.getString("metrics.name").getOrElse("default")

  def startGraphite(): Unit = {
    Logger.info("Graphite metrics enabled, starting the reporter")

    val metricsConfig = microserviceMetricsConfig.getOrElse(throw new Exception("The application does not contain required metrics configuration"))

    val defaultGraphitePort = 2003
    val defaultGraphiteInterval = 10L

    val graphite = new Graphite(new InetSocketAddress(
      metricsConfig.getString("graphite.host").getOrElse("graphite"),
      metricsConfig.getInt("graphite.port").getOrElse(defaultGraphitePort)))

    val prefix = metricsConfig.getString("graphite.prefix").getOrElse(s"tax.${app.configuration.getString("appName")}")

    val reporter = GraphiteReporter.forRegistry(
      SharedMetricRegistries.getOrCreate(registryName))
      .prefixedWith(s"$prefix.${java.net.InetAddress.getLocalHost.getHostName}")
      .convertRatesTo(SECONDS)
      .convertDurationsTo(MILLISECONDS)
      .filter(MetricFilter.ALL)
      .build(graphite)

    reporter.start(metricsConfig.getLong("graphite.interval").getOrElse(defaultGraphiteInterval), SECONDS)
  }

}

