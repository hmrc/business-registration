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

import sbt._

object AppDependencies {

  def tmpMacWorkaround(): Seq[ModuleID] =
    if (sys.props.get("os.name").exists(_.toLowerCase.contains("mac")))
      Seq("org.reactivemongo" % "reactivemongo-shaded-native" % "0.17.1-osx-x86-64" % "runtime,test,it")
    else Seq()

  def apply(): Seq[ModuleID] = MainDependencies() ++ UnitTestDependencies() ++ IntegrationTestDependencies() ++ tmpMacWorkaround()
}

object MainDependencies {

  private val simpleReactive    = "7.22.0-play-25"
  private val bootstrap         = "5.1.0"
  private val domain            = "5.6.0-play-25"
  private val scheduling        = "7.1.0-play-25"
  private val mongoLock         = "6.15.0-play-25"
  private val authClientVersion = "2.32.0-play-25"

  def apply(): Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "simple-reactivemongo"     % simpleReactive,
    "uk.gov.hmrc" %% "bootstrap-play-25"        % bootstrap,
    "uk.gov.hmrc" %% "domain"                   % domain,
    "uk.gov.hmrc" %% "play-scheduling"          % scheduling,
    "uk.gov.hmrc" %% "mongo-lock"               % mongoLock,
    "uk.gov.hmrc" %% "auth-client"              % authClientVersion
  )
}

trait CommonTestDependencies {
  val hmrcTestVersion       = "3.9.0-play-25"
  val scalaTestPlusVersion  = "2.0.1"
  val mockitoCore           = "2.13.0"
  val reactiveMongo         = "4.15.0-play-25"
  val wireMockVersion       = "2.9.0"

  val scope: Configuration
  val dependencies : Seq[ModuleID]
}

object UnitTestDependencies extends CommonTestDependencies {
  override val scope = Test
  override val dependencies = Seq(
    "uk.gov.hmrc"             %% "hmrctest"           % hmrcTestVersion       % scope,
    "org.scalatestplus.play"  %% "scalatestplus-play" % scalaTestPlusVersion  % scope,
    "uk.gov.hmrc"             %% "reactivemongo-test" % reactiveMongo         % scope,
    "org.mockito"             %  "mockito-core"       % mockitoCore           % scope
  )

  def apply() = dependencies
}

object IntegrationTestDependencies extends CommonTestDependencies {
  override val scope = IntegrationTest
  override val dependencies = Seq(
    "uk.gov.hmrc"             %% "hmrctest"           % hmrcTestVersion       % scope,
    "org.scalatestplus.play"  %% "scalatestplus-play" % scalaTestPlusVersion  % scope,
    "uk.gov.hmrc"             %% "reactivemongo-test" % reactiveMongo         % scope,
    "com.github.tomakehurst"  %  "wiremock"           % wireMockVersion       % scope
  )

  def apply(): Seq[ModuleID] = dependencies
}
