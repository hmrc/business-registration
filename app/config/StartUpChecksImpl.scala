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

import javax.inject.{Inject, Singleton}

import play.api.Configuration
import services.MetadataService
import java.util.Base64

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._



class StartUpChecksImpl @Inject()(val metadataService: MetadataService, val config: Configuration) extends StartUpChecks {
  lazy val regIdConf = config.getString("registrationList").getOrElse("")
}

trait StartUpChecks {
  val metadataService: MetadataService
  val config: Configuration
  val regIdConf: String
  implicit val hc: HeaderCarrier = HeaderCarrier()

  lazy val regIdList = new String(Base64.getDecoder.decode(regIdConf), "UTF-8")

  metadataService.checkCompletionCapacity(regIdList.split(","))

}
