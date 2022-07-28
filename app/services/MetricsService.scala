/*
 * Copyright 2022 HM Revenue & Customs
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

package services

import javax.inject.{Inject, Singleton}

import com.codahale.metrics.{Counter, Timer}
import com.kenshoo.play.metrics.Metrics


@Singleton
class MetricsService @Inject()(metrics: Metrics) {

  lazy val keystoreReadTimer: Timer = metrics.defaultRegistry.timer("keystore-read-timer")
  lazy val keystoreWriteTimer: Timer = metrics.defaultRegistry.timer("keystore-write-timer")

  lazy val keystoreReadFailed: Counter = metrics.defaultRegistry.counter("keystore-read-failed-counter")
  lazy val keystoreWriteFailed: Counter = metrics.defaultRegistry.counter("keystore-write-failed-counter")

  lazy val keystoreHitCounter: Counter = metrics.defaultRegistry.counter("keystore-hit-counter")
  lazy val keystoreMissCounter: Counter = metrics.defaultRegistry.counter("keystore-miss-counter")

  lazy val identityVerificationTimer: Timer = metrics.defaultRegistry.timer("identity-verification-timer")
  lazy val identityVerificationFailedCounter: Counter = metrics.defaultRegistry.counter("identity-verification-failed-counter")
  lazy val createMetadataTimer: Timer = metrics.defaultRegistry.timer("create-metadata-in-br-timer")
  lazy val searchMetadataTimer: Timer = metrics.defaultRegistry.timer("search-metadata-in-br-timer")
  lazy val retrieveMetadataTimer: Timer = metrics.defaultRegistry.timer("retrieve-metadata-in-br-timer")
  lazy val removeMetadataTimer: Timer = metrics.defaultRegistry.timer("remove-metadata-in-br-timer")
  lazy val updateMetadataTimer: Timer = metrics.defaultRegistry.timer("update-metadata-in-br-timer")

  lazy val createFootprintCounter: Counter = metrics.defaultRegistry.counter("number-of-footprints-created-counter")

}
