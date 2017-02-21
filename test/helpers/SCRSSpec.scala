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

package helpers

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}

//todo: Scalatest 3.0.1 is not compatible with Scalatestplus-play 1.5.x. The version that it's built against is "org.scalatest" %% "scalatest" % "2.2.6".
//todo: OneAppPerSuite will throw ClassCastExceptions when trying to stop the application after a suite of tests
//todo: see https://github.com/playframework/scalatestplus-play/issues/55
trait SCRSSpec extends UnitSpec with MockitoSugar with WithFakeApplication {
  implicit val actorSystem: ActorSystem = fakeApplication.actorSystem
  implicit val materializer: Materializer = fakeApplication.materializer
}
