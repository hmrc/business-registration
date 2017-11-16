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

import sbt._

object AppDependencies {
  def apply(): Seq[ModuleID] = MainDependencies() ++ UnitTestDependencies() ++ IntegrationTestDependencies()
}

object MainDependencies {
  private val playReactiveMongo = "5.2.0"
  private val bootstrap         = "6.13.0"
  private val urlBinders        = "2.1.0"
  private val domain            = "5.0.0"
  private val scheduling        = "4.1.0"
  private val mongoLock         = "5.0.0"

  def apply(): Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "play-reactivemongo"     % playReactiveMongo,
    "uk.gov.hmrc" %% "microservice-bootstrap" % bootstrap,
    "uk.gov.hmrc" %% "play-url-binders"       % urlBinders,
    "uk.gov.hmrc" %% "domain"                 % domain,
    "uk.gov.hmrc" %% "play-scheduling"        % scheduling,
    "uk.gov.hmrc" %% "mongo-lock"             % mongoLock
  )
}

trait CommonTestDependencies {
  val hmrcTestVersion       = "2.3.0"
  val scalaTestPlusVersion  = "2.0.1"
  val mockitoCore           = "2.12.0"
  val reactiveMongo         = "2.0.0"

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
    "uk.gov.hmrc"             %% "reactivemongo-test" % reactiveMongo         % scope
  )

  def apply(): Seq[ModuleID] = dependencies
}
