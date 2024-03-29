/*
 * Copyright 2020 HM Revenue & Customs
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

import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, integrationTestSettings, scalaSettings}
import uk.gov.hmrc.bobby.SbtBobbyPlugin.BobbyKeys.bobbyRulesURL
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.projectSettings
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "business-registration"

lazy val scoverageSettings = Seq(
  ScoverageKeys.coverageExcludedPackages := "<empty>;controllers.beta.*;Reverse.*;model.*;config.*;.*(AuthService|BuildInfo|Routes).*",
  ScoverageKeys.coverageMinimumStmtTotal := 80,
  ScoverageKeys.coverageFailOnMinimum := true,
  ScoverageKeys.coverageHighlighting := true
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(PlayScala, SbtAutoBuildPlugin, SbtDistributablesPlugin): _*)
  .settings(scoverageSettings: _*)
  .settings(scalaSettings: _*)
  .settings(projectSettings: _*)
  .settings(defaultSettings(): _*)
  .configs(IntegrationTest)
  .settings(integrationTestSettings())
  .settings(majorVersion := 1)
  .settings(bobbyRulesURL := Some(new URL("https://webstore.tax.service.gov.uk/bobby-config/deprecated-dependencies.json")))
  .settings(
    scalacOptions += "-Xlint:-unused",
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    routesGenerator := InjectedRoutesGenerator,
    scalaVersion := "2.13.8",
    Test / Keys.fork := true
  )