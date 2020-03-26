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

import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  val tmpMacWorkaround: Seq[ModuleID] =
    if (sys.props.get("os.name").exists(_.toLowerCase.contains("mac")))
      Seq("org.reactivemongo" % "reactivemongo-shaded-native" % "0.17.1-osx-x86-64" % "runtime,test,it")
    else
      Seq()

  private val simpleReactive = "7.26.0-play-26"
  private val bootstrap = "1.6.0"
  private val domain = "5.6.0-play-26"
  private val scheduling = "7.4.0-play-26"
  private val mongoLock = "6.21.0-play-26"
  private val authClientVersion = "2.35.0-play-26"
  private val scalaTestPlusVersion = "3.1.3"
  private val mockitoCore = "2.13.0"
  private val reactiveMongo = "4.19.0-play-26"
  private val wireMockVersion = "2.26.3"
  private val playJsonVersion = "2.6.14"

  val compile: Seq[ModuleID] = Seq(
    "com.typesafe.play" %% "play-json-joda" % playJsonVersion,
    "uk.gov.hmrc" %% "simple-reactivemongo" % simpleReactive,
    "uk.gov.hmrc" %% "bootstrap-play-26" % bootstrap,
    "uk.gov.hmrc" %% "domain" % domain,
    "uk.gov.hmrc" %% "play-scheduling" % scheduling,
    "uk.gov.hmrc" %% "mongo-lock" % mongoLock,
    "uk.gov.hmrc" %% "auth-client" % authClientVersion,
    ws
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % "test, it",
    "uk.gov.hmrc" %% "reactivemongo-test" % reactiveMongo % "test, it",
    "org.mockito" % "mockito-core" % mockitoCore % "test",
    "com.github.tomakehurst" % "wiremock-jre8" % wireMockVersion % "it"
  )

  def apply(): Seq[ModuleID] = compile ++ test ++ tmpMacWorkaround

}
