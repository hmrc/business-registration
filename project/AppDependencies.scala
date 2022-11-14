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

  private val playVersion           = "-play-28"

  private val bootstrap             =  "7.11.0"
  private val domain                = s"8.1.0$playVersion"
  private val scalaTestPlusVersion  =  "5.1.0"
  private val wireMockVersion       =  "2.33.2"
  private val hmrcMongoVersion      =  "0.73.0"
  private val scalaTestVersion      =  "3.2.12"
  private val flexmarkAllVersion    =  "0.62.2"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo"             %% s"hmrc-mongo$playVersion"          % hmrcMongoVersion,
    "uk.gov.hmrc"                   %% s"bootstrap-backend$playVersion"   % bootstrap,
    "uk.gov.hmrc"                   %%  "domain"                          % domain,
    ws
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"                 %%  "scalatest"                       % scalaTestVersion        % "test, it",
    "org.scalatestplus.play"        %%  "scalatestplus-play"              % scalaTestPlusVersion    % "test, it",
    "com.vladsch.flexmark"          %   "flexmark-all"                    % flexmarkAllVersion      % "test, it",
    "uk.gov.hmrc.mongo"             %% s"hmrc-mongo-test$playVersion"     % hmrcMongoVersion        % "test, it",
    "org.scalatestplus"             %%  "mockito-4-5"                     % s"$scalaTestVersion.0"  % "test, it",
    "com.github.tomakehurst"        %   "wiremock-jre8-standalone"        % wireMockVersion         % "it"
  )

  def apply(): Seq[ModuleID] = compile ++ test

}
