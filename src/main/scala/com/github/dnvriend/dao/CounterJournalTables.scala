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

import com.github.dnvriend.dao.CounterJournalTables.EventType.EventType

object CounterJournalTables {
  final case class JournalRow(persistenceId: String, sequenceNumber: Long, eventType: EventType.EventType, created: Long, tags: Option[String] = None)
  final case class JournalDeletedToRow(persistenceId: String, deletedTo: Long)
  final case class IncrementedRow(persistenceId: String, sequenceNumber: Long, incrementedBy: Int)
  final case class DecrementedRow(persistenceId: String, sequenceNumber: Long, decrementedBy: Int)

  object EventType extends Enumeration {
    type EventType = Value
    val Incremented = Value("INCREMENTED")
    val Decremented = Value("DECREMENTED")
  }
}

trait CounterJournalTables {
  import CounterJournalTables._

  import akka.persistence.postgres.db.ExtendedPostgresProfile.api._

  class JournalTable(_tableTag: Tag) extends Table[JournalRow](_tableTag, _schemaName = Option("counter"), _tableName = "event_log") {
    def * = (persistenceId, sequenceNumber, eventType, created, tags) <> (JournalRow.tupled, JournalRow.unapply)

    val persistenceId: Rep[String] = column[String]("persistence_id", O.Length(255, varying = true))
    val sequenceNumber: Rep[Long] = column[Long]("sequence_number")
    val created: Rep[Long] = column[Long]("created")
    val tags: Rep[Option[String]] = column[String]("tags", O.Length(255, varying = true))
    val eventType: Rep[EventType.EventType] = column[EventType.EventType]("event_type")
    val pk = primaryKey("event_log_pk", (persistenceId, sequenceNumber))
  }

  implicit val eventTypeEnumMapper = MappedColumnType.base[EventType, String](
    e ⇒ e.toString,
    s ⇒ EventType.withName(s))

  lazy val JournalTable = new TableQuery(tag ⇒ new JournalTable(tag))

  class JournalDeletedTo(_tableTag: Tag) extends Table[JournalDeletedToRow](_tableTag, _schemaName = Option("counter"), _tableName = "event_log_deleted_to") {
    def * = (persistenceId, deletedTo) <> (JournalDeletedToRow.tupled, JournalDeletedToRow.unapply)

    val persistenceId: Rep[String] = column[String]("persistence_id", O.Length(255, varying = true))
    val deletedTo: Rep[Long] = column[Long]("deleted_to")
  }

  lazy val JournalDeletedToTable = new TableQuery(tag ⇒ new JournalDeletedTo(tag))

  class IncrementedTable(_tableTag: Tag) extends Table[IncrementedRow](_tableTag, _schemaName = Some("counter"), _tableName = "incremented") {
    def * = (persistenceId, sequenceNumber, incrementedBy) <> (IncrementedRow.tupled, IncrementedRow.unapply)

    val persistenceId: Rep[String] = column[String]("persistence_id", O.Length(255, varying = true))
    val sequenceNumber: Rep[Long] = column[Long]("sequence_number")
    val incrementedBy: Rep[Int] = column[Int]("incremented_by")
    val pk = primaryKey("incremented_pk", (persistenceId, sequenceNumber))
    lazy val incrementedFk = foreignKey("incr_el_fk", (persistenceId, sequenceNumber), JournalTable)(r ⇒ (r.persistenceId, r.sequenceNumber), onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)
  }

  lazy val IncrementedTable = new TableQuery(tag ⇒ new IncrementedTable(tag))

  class DecrementedTable(_tableTag: Tag) extends Table[DecrementedRow](_tableTag, _schemaName = Some("counter"), _tableName = "decremented") {
    def * = (persistenceId, sequenceNumber, decrementedBy) <> (DecrementedRow.tupled, DecrementedRow.unapply)

    val persistenceId: Rep[String] = column[String]("persistence_id", O.Length(255, varying = true))
    val sequenceNumber: Rep[Long] = column[Long]("sequence_number")
    val decrementedBy: Rep[Int] = column[Int]("decremented_by")
    val pk = primaryKey("decremented_pk", (persistenceId, sequenceNumber))
    lazy val decrementedFk = foreignKey("decr_el_fk", (persistenceId, sequenceNumber), JournalTable)(r ⇒ (r.persistenceId, r.sequenceNumber), onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)
  }

  lazy val DecrementedTable = new TableQuery(tag ⇒ new DecrementedTable(tag))
}
