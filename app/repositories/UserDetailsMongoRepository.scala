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

package repositories

import models.{Response, WhiteListDetailsSubmit}
import play.api.Logger
import reactivemongo.api.DB
import reactivemongo.bson.{BSONDocument, BSONObjectID, BSONString}
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.{ReactiveRepository, Repository}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

trait UserDetailsRepository extends Repository[WhiteListDetailsSubmit, BSONObjectID]{
  def createRegistration(details : WhiteListDetailsSubmit) : Future[WhiteListDetailsSubmit]
  def searchRegistration(email : String) : Future[Option[WhiteListDetailsSubmit]]
  def emailSelector(email : String) : BSONDocument
  def removeBetaUsers() : Future[Option[Response]]
}

class UserDetailsMongoRepository(implicit mongo: () => DB)
  extends ReactiveRepository[WhiteListDetailsSubmit, BSONObjectID](Collections.userdata, mongo, WhiteListDetailsSubmit.format, ReactiveMongoFormats.objectIdFormats)
    with UserDetailsRepository {

  override def createRegistration(details: WhiteListDetailsSubmit) : Future[WhiteListDetailsSubmit] = {
    collection.insert(details).map { res =>
      details
    }
  }

  override def emailSelector(email: String) : BSONDocument = BSONDocument("email" -> BSONString(email))

  override def searchRegistration(email: String) : Future[Option[WhiteListDetailsSubmit]] = {
    collection.find(emailSelector(email)).one[WhiteListDetailsSubmit]
  }

  override def removeBetaUsers(): Future[Option[Response]] = {
    collection.drop().map {
      resp => Some(Response("Dropped"))
    }
  }
}
