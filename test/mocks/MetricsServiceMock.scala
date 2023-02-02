/*
 * Copyright 2023 HM Revenue & Customs
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

package mocks

import com.codahale.metrics.{Counter, Timer}
import com.kenshoo.play.metrics.Metrics
import org.scalatestplus.mockito.MockitoSugar
import services.MetricsService

trait MetricsServiceMock {
  this: MockitoSugar =>

  val metrics = mock[Metrics]

  val mockMetricsService = new MetricsService(metrics) {

    val mockTimer: Timer = new Timer()
    val mockCounter: Counter = mock[Counter]

    override lazy val keystoreReadTimer: Timer = mockTimer
    override lazy val keystoreWriteTimer: Timer = mockTimer
    override lazy val keystoreReadFailed: Counter = mockCounter
    override lazy val keystoreWriteFailed: Counter = mockCounter
    override lazy val keystoreHitCounter: Counter = mockCounter
    override lazy val keystoreMissCounter: Counter = mockCounter
    override lazy val identityVerificationTimer: Timer = mockTimer
    override lazy val identityVerificationFailedCounter: Counter = mockCounter
    override lazy val createMetadataTimer: Timer = mockTimer
    override lazy val searchMetadataTimer: Timer = mockTimer
    override lazy val retrieveMetadataTimer: Timer = mockTimer
    override lazy val removeMetadataTimer: Timer = mockTimer
    override lazy val updateMetadataTimer: Timer = mockTimer
    override lazy val createFootprintCounter: Counter = mockCounter
  }
}
