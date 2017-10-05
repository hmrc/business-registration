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

package config

import com.google.inject.AbstractModule
import config.filters.{MicroserviceAuditConnector, MicroserviceHttp}
import connectors.{AuthConnector, AuthConnectorImpl}
import controllers.{MetadataController, MetadataControllerImpl}
import controllers.admin.{AdminController, AdminControllerImpl}
import controllers.prePop.{AddressController, AddressControllerImpl, ContactDetailsController, ContactDetailsControllerImpl}
import controllers.test.{MetadataTestController, MetadataTestControllerImpl}
import services.prepop.{AddressService, AddressServiceImpl}
import services.{MetricsService, MetricsServiceImp}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.ws.WSHttp

class DIModule extends AbstractModule {

  protected def configure(): Unit = {

    //config
    bind(classOf[WSHttp]).to(classOf[MicroserviceHttp])
    bind(classOf[AuditConnector]).to(classOf[MicroserviceAuditConnector])

    //controllers
    bind(classOf[AddressController]).to(classOf[AddressControllerImpl])
    bind(classOf[ContactDetailsController]).to(classOf[ContactDetailsControllerImpl])
    bind(classOf[AdminController]).to(classOf[AdminControllerImpl])
    bind(classOf[MetadataController]).to(classOf[MetadataControllerImpl])
    bind(classOf[MetadataTestController]).to(classOf[MetadataTestControllerImpl])

    //services
    bind(classOf[AddressService]).to(classOf[AddressServiceImpl])
    bind(classOf[MetricsService]).to(classOf[MetricsServiceImp])

    //connectors
    bind(classOf[AuthConnector]).to(classOf[AuthConnectorImpl])

  }
}
