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

import com.google.inject.AbstractModule
import controllers.admin.AdminController
import controllers.prePop.{AddressController, AddressControllerImpl, ContactDetailsController, ContactDetailsControllerImpl}
import controllers.test.{BRMongoTestController, MetadataTestControllerImpl}
import controllers.{MetadataController, MetadataControllerImpl}
import repositories.prepop.ContactDetailsMongo
import repositories.{MetadataMongo, SequenceRepository, SequenceRepositoryImpl}
import services.prepop.{AddressService, AddressServiceImpl}
import services.{MetricsService, MetricsServiceImp}

class DIModule extends AbstractModule {

  protected def configure() = {
    configureConfig()
    configureControllers()
    configureServices()
    configureRepositories()
  }

  def configureControllers(): Unit = {
    bind(classOf[AddressController]).to(classOf[AddressControllerImpl]).asEagerSingleton()
    bind(classOf[ContactDetailsController]).to(classOf[ContactDetailsControllerImpl]).asEagerSingleton()
    bind(classOf[MetadataController]).to(classOf[MetadataControllerImpl]).asEagerSingleton()
    bind(classOf[BRMongoTestController]).to(classOf[MetadataTestControllerImpl]).asEagerSingleton()
    bind(classOf[AdminController]).asEagerSingleton()
  }

  def configureConfig(): Unit = {
    bind(classOf[StartUpChecks]).to(classOf[StartUpChecksImpl]).asEagerSingleton()
  }

  def configureServices(): Unit = {
    bind(classOf[AddressService]).to(classOf[AddressServiceImpl]).asEagerSingleton()
    bind(classOf[MetricsService]).to(classOf[MetricsServiceImp]).asEagerSingleton()
  }


  def configureRepositories(): Unit = {
    bind(classOf[MetadataMongo]).asEagerSingleton()
    bind(classOf[SequenceRepository]).to(classOf[SequenceRepositoryImpl]).asEagerSingleton()
    bind(classOf[ContactDetailsMongo]).asEagerSingleton()
  }
}
