/*
 * Copyright 2016 Dennis Vriend
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

package com.github.dnvriend.dao

import akka.persistence.PersistentRepr
import akka.persistence.jdbc.config.JournalConfig
import akka.persistence.jdbc.dao.bytea.journal.JournalTables.JournalRow
import akka.persistence.jdbc.dao.bytea.journal.{ ByteArrayJournalDao, ByteArrayJournalSerializer }
import akka.serialization.Serialization
import akka.stream.Materializer
import com.github.dnvriend.adapter.Wrapper
import slick.driver.JdbcProfile
import slick.jdbc.JdbcBackend._

import scala.concurrent.ExecutionContext
import scala.util.Try

class WrapperByteArrayJournalDao(db: Database, profile: JdbcProfile, journalConfig: JournalConfig, serialization: Serialization)(implicit ec: ExecutionContext, mat: Materializer) extends ByteArrayJournalDao(db, profile, journalConfig, serialization) {
  override val serializer: ByteArrayJournalSerializer = new ByteArrayJournalSerializer(serialization, journalConfig.pluginConfig.tagSeparator) {
    override def deserialize(journalRow: JournalRow): Try[(PersistentRepr, Set[String])] = {
      super.deserialize(journalRow)
        .map {
          case (repr, tags) ⇒ (repr.withPayload(Wrapper(repr.payload, journalRow.created)), tags)
        }
    }
  }
}
