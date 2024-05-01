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

  private val playVersion           = "-play-30"

  private val bootstrap             =  "8.5.0"
  private val domain                =  "9.0.0"
  private val scalaTestPlusVersion  =  "7.0.1"
  private val hmrcMongoVersion      =  "1.9.0"
  private val flexmarkAllVersion    =  "0.64.8"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo"             %% s"hmrc-mongo$playVersion"          % hmrcMongoVersion,
    "uk.gov.hmrc"                   %% s"bootstrap-backend$playVersion"   % bootstrap,
    "uk.gov.hmrc"                   %%  s"domain$playVersion"             % domain,
    ws
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-test-play-30"           % bootstrap,
    "uk.gov.hmrc.mongo"             %% s"hmrc-mongo-test$playVersion"     % hmrcMongoVersion,
    "org.scalatestplus.play"        %%  "scalatestplus-play"              % scalaTestPlusVersion,
    "com.vladsch.flexmark"          %   "flexmark-all"                    % flexmarkAllVersion,
    "org.scalatestplus"             %%  "mockito-4-5"                     % "3.2.12.0"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test

}
