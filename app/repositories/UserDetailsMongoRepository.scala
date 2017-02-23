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

import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import models.{Response, WhiteListDetailsSubmit}
import play.api.Application
import play.api.libs.json.Format
import reactivemongo.api.DB
import reactivemongo.bson.{BSONDocument, BSONObjectID, BSONString}
import repositories.CollectionsNames._
import InjectDB.injectDB
import uk.gov.hmrc.mongo.{ReactiveRepository, Repository}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@ImplementedBy(classOf[UserDetailsRepositoryImpl])
trait UserDetailsRepository extends Repository[WhiteListDetailsSubmit, BSONObjectID]{
  def createRegistration(details : WhiteListDetailsSubmit) : Future[WhiteListDetailsSubmit]
  def searchRegistration(email : String) : Future[Option[WhiteListDetailsSubmit]]
  def emailSelector(email : String) : BSONDocument
  def removeBetaUsers() : Future[Option[Response]]
}

abstract class UserDetailsRepositoryBase(mongo: () => DB)(implicit formats: Format[WhiteListDetailsSubmit], manifest: Manifest[WhiteListDetailsSubmit])
  extends ReactiveRepository[WhiteListDetailsSubmit, BSONObjectID](USER_DATA, mongo, formats)
    with UserDetailsRepository

@Singleton
class UserDetailsRepositoryImpl @Inject()(implicit app: Application) extends UserDetailsRepositoryBase(injectDB(app)) {

  override def createRegistration(details: WhiteListDetailsSubmit) = collection.insert(details).map(_ => details)

  override def emailSelector(email: String) = BSONDocument("email" -> BSONString(email))

  override def searchRegistration(email: String) = collection.find(emailSelector(email)).one[WhiteListDetailsSubmit]

  override def removeBetaUsers() = collection.drop().map(_ => Some(Response("Dropped")))
}
