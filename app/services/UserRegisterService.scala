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

package services

import javax.inject.{Inject, Singleton}

import models.{ErrorResponse, Response, WhiteListDetailsSubmit}
import play.api.libs.json.Json
import repositories.{UserDetailsMongo, UserDetailsRepository}
import play.api.mvc.Result
import play.api.mvc.Results.{Created, NotFound, Ok}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

//class UserRegisterServiceImp @Inject() (repositories: Repositories) extends UserRegisterService {
//  val userDetailsRepository = repositories.userDetailsRepository
//}

@Singleton
class UserRegisterService @Inject() (userDetails: UserDetailsMongo){

  val repository = userDetails.repository

  def createRegistration(details : WhiteListDetailsSubmit) : Future[Result] = {
    repository.createRegistration(details).map(res => Created(Json.toJson(res)))
  }

  def searchRegistrations(email : String) : Future[Result] = {
    repository.searchRegistration(email).map {
      case Some(data) => Ok(Json.toJson[WhiteListDetailsSubmit](data))
      case _ => NotFound(ErrorResponse.UserNotFound)
    }
  }

  def dropUsers() : Future[Result] = {
    repository.removeBetaUsers().map {
      resp => Ok(Json.toJson[Response](resp.get))
    }
  }
}
