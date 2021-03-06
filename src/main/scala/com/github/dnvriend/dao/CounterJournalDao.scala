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

import akka.NotUsed
import akka.persistence.jdbc.config.JournalConfig
import akka.persistence.jdbc.dao.JournalDao
import akka.persistence.{ AtomicWrite, PersistentRepr }
import akka.serialization.Serialization
import akka.stream.Materializer
import akka.stream.scaladsl.{ Flow, Source }
import com.github.dnvriend.CounterActor.{ Decremented, Incremented }
import com.github.dnvriend.dao.CounterJournalTables.{ DecrementedRow, EventType, IncrementedRow, JournalRow }
import slick.driver.JdbcProfile
import slick.jdbc.JdbcBackend

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

class CounterJournalDao(db: JdbcBackend#Database, val profile: JdbcProfile, journalConfig: JournalConfig, serialization: Serialization)(implicit ec: ExecutionContext, mat: Materializer) extends JournalDao with CounterJournalTables {

  import profile.api._

  override def delete(persistenceId: String, toSequenceNr: Long): Future[Unit] = ???

  override def messages(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long, max: Long): Source[Try[PersistentRepr], NotUsed] = {
    val messagesQuery = JournalTable
      .filter(_.persistenceId === persistenceId)
      .filter(_.sequenceNumber >= fromSequenceNr)
      .filter(_.sequenceNumber <= toSequenceNr)
      .sortBy(_.sequenceNumber.asc)
      .take(max)
    Source.fromPublisher(db.stream(messagesQuery.result))
      .mapAsync(1) {
        case JournalRow(pid, seqno, EventType.Incremented, _, _) ⇒
          db.run(IncrementedTable.filter(_.persistenceId === pid).filter(_.sequenceNumber === seqno).result)
            .map(_.head)
            .map(row ⇒ Success(PersistentRepr(Incremented(row.incrementedBy), seqno)))
        case JournalRow(pid, seqno, EventType.Decremented, _, _) ⇒
          db.run(DecrementedTable.filter(_.persistenceId === pid).filter(_.sequenceNumber === seqno).result)
            .map(_.head)
            .map(row ⇒ Success(PersistentRepr(Decremented(row.decrementedBy), seqno)))
      }
  }

  override def highestSequenceNr(persistenceId: String, fromSequenceNr: Long): Future[Long] = {
    val actions = (for {
      seqNumFoundInJournalTable ← JournalTable.filter(_.persistenceId === persistenceId).filter(_.sequenceNumber >= fromSequenceNr).map(_.sequenceNumber).max.result
      highestSeqNumberFoundInDeletedToTable ← JournalDeletedToTable.filter(_.persistenceId === persistenceId).map(_.deletedTo).max.result
      highestSequenceNumber = seqNumFoundInJournalTable.getOrElse(highestSeqNumberFoundInDeletedToTable.getOrElse(0L))
    } yield highestSequenceNumber).transactionally
    db.run(actions)
  }

  override def writeFlow: Flow[AtomicWrite, Try[Unit], NotUsed] =
    Flow[AtomicWrite].map(_.payload).mapAsync(1)(persistListOfRepr)

  def persistListOfRepr(reprs: Seq[PersistentRepr]): Future[Try[Unit]] = {
    val xx = reprs.map {
      case repr @ PersistentRepr(Incremented(value), seqno) ⇒
        for {
          _ ← JournalTable += JournalRow(repr.persistenceId, seqno, EventType.Incremented, System.currentTimeMillis())
          _ ← IncrementedTable += IncrementedRow(repr.persistenceId, seqno, value)
        } yield ()
      case repr @ PersistentRepr(Decremented(value), seqno) ⇒
        for {
          _ ← JournalTable += JournalRow(repr.persistenceId, seqno, EventType.Decremented, System.currentTimeMillis())
          _ ← DecrementedTable += DecrementedRow(repr.persistenceId, seqno, value)
        } yield ()
    }
    db.run(DBIO.sequence(xx).transactionally).map(_ ⇒ Success(())).recover {
      case t: Throwable ⇒
        t.printStackTrace()
        Failure(t)
    }
  }
}
