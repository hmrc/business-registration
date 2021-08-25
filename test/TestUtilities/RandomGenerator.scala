/*
 * Copyright 2021 HM Revenue & Customs
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

package TestUtilities

import scala.util.Random

object RandomGenerator {

  def generateName(length : Int) : String = Random.alphanumeric.take(length).mkString

  def generateEmail(firstName : Int, lastName : Int) : String =
    s"${Random.alphanumeric.take(firstName).mkString}.${Random.alphanumeric.take(lastName).mkString}@email.com"

  def trimEmail(email : String) : String = email.replaceAll(".com", ".co")

  def generatePhone() : String = {
    val low = 100000000
    val high = 999999999
    s"0${Random.nextInt(high - low) + low}"
  }
}
