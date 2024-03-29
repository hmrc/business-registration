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

package auth

import scala.concurrent.{ExecutionContext, Future}

trait AuthorisationResource {

  /**
    * @param id - The registration id of the logged in user
    * @return I - The registration id of the logged in user
    *         String - The internal ID of the logged in user
    */
  def getInternalId(id: String)(implicit ec: ExecutionContext): Future[Option[String]]

  def getInternalIds(registrationId: String)(implicit ec: ExecutionContext): Future[Seq[String]] = Future.successful(Seq.empty)
}
