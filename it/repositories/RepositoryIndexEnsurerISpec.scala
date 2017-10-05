/*
 * Copyright 2016 HM Revenue & Customs
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

import config.RepositoryIndexEnsurer
import helpers.MongoSpec
import reactivemongo.api.indexes.{Index, IndexType}
import repositories.prepop.{ContactDetailsMongo, ContactDetailsRepoMongo}

import scala.concurrent.Future


class RepositoryIndexEnsurerISpec extends MongoSpec {

  class Setup {
    val indexEnsurer = RepositoryIndexEnsurer(fakeApplication)
    val repo : ContactDetailsRepoMongo = fakeApplication.injector.instanceOf[ContactDetailsMongo].repository
    await(repo.ensureIndexes)
    repo.dropIndexes
  }

  "RepositoryIndexEnsurer" should {
    val index = Index(Seq(("temp", IndexType.Descending)), Some("uniqueIntID"))

    "be able to remove an index from the database collection" in new Setup {
      repo.listIndexes.size shouldBe 1
      await(repo.collection.indexesManager.create(index))
      repo.listIndexes.size shouldBe 2
      await(indexEnsurer.deleteIndexes("uniqueIntID"))
      repo.listIndexes.size shouldBe 1
      await(repo.collection.indexesManager.list()).exists(i => i.eventualName == index.eventualName) shouldBe false
    }
  }
}
