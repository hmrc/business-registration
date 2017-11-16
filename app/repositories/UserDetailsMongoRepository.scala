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

import models.{Response, WhiteListDetailsSubmit}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.DB
import reactivemongo.bson.{BSONDocument, BSONObjectID, BSONString}
import reactivemongo.play.json._
import repositories.CollectionsNames._
import uk.gov.hmrc.mongo.{ReactiveRepository, Repository}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserDetailsMongo @Inject()(mongo: ReactiveMongoComponent) {
  val repository = new UserDetailsRepositoryMongo(mongo.mongoConnector.db)
}

trait UserDetailsRepository extends Repository[WhiteListDetailsSubmit, BSONObjectID]{
  def createRegistration(details : WhiteListDetailsSubmit)(implicit ec: ExecutionContext): Future[WhiteListDetailsSubmit]
  def searchRegistration(email : String)(implicit ec: ExecutionContext): Future[Option[WhiteListDetailsSubmit]]
  def removeBetaUsers(implicit ec: ExecutionContext): Future[Option[Response]]
}


class UserDetailsRepositoryMongo (mongo: () => DB) extends ReactiveRepository[WhiteListDetailsSubmit, BSONObjectID](METADATA, mongo, WhiteListDetailsSubmit.format)
  with UserDetailsRepository{

  override def createRegistration(details: WhiteListDetailsSubmit)(implicit ec: ExecutionContext) = collection.insert(details).map(_ => details)

  override def searchRegistration(email: String)(implicit ec: ExecutionContext) = collection.find(emailSelector(email)).one[WhiteListDetailsSubmit]

  override def removeBetaUsers(implicit ec: ExecutionContext) = collection.drop().map(_ => Some(Response("Dropped")))

  private def emailSelector(email: String) = BSONDocument("email" -> BSONString(email))
}
