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
import akka.actor.ActorSystem
import akka.persistence.PersistentRepr
import akka.persistence.jdbc.dao.JournalDao
import akka.persistence.jdbc.serialization.{ NotSerialized, SerializationResult }
import akka.stream.scaladsl.{ Flow, Source }
import akka.stream.{ ActorMaterializer, Materializer }
import com.github.dnvriend.CounterActor.{ Decremented, Incremented }
import com.github.dnvriend.dao.CounterJournalTables.{ IncrementedRow, DecrementedRow, JournalRow, EventType }
import slick.driver.JdbcProfile
import slick.jdbc.JdbcBackend

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

class CounterJournalDao(db: JdbcBackend#Database, val profile: JdbcProfile, system: ActorSystem) extends JournalDao with CounterJournalTables {
  import profile.api._

  implicit val ec: ExecutionContext = system.dispatcher

  implicit val mat: Materializer = ActorMaterializer()(system)

  println("===> Creating CounterJournalDao: " + this.hashCode() + "actorsystem: " + system.hashCode())

  override def allPersistenceIdsSource: Source[String, NotUsed] =
    Source.fromPublisher(db.stream(JournalTable.map(_.persistenceId).distinct.result))

  override def eventsByPersistenceIdAndTag(persistenceId: String, tag: String, offset: Long): Source[SerializationResult, NotUsed] = ???

  override def eventsByTag(tag: String, offset: Long): Source[SerializationResult, NotUsed] = ???

  override def delete(persistenceId: String, toSequenceNr: Long): Future[Unit] = ???

  override def persistenceIds(queryListOfPersistenceIds: Iterable[String]): Future[Seq[String]] = {
    val query = for {
      persistenceIdsInJournal ← JournalTable.map(_.persistenceId)
      if persistenceIdsInJournal inSetBind queryListOfPersistenceIds
    } yield persistenceIdsInJournal
    db.run(query.result)
  }

  override def messages(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long, max: Long): Source[SerializationResult, NotUsed] = {
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
            .map(row ⇒ NotSerialized(pid, seqno, PersistentRepr(Incremented(row.incrementedBy), seqno), None))
        case JournalRow(pid, seqno, EventType.Decremented, _, _) ⇒
          db.run(DecrementedTable.filter(_.persistenceId === pid).filter(_.sequenceNumber === seqno).result)
            .map(_.head)
            .map(row ⇒ NotSerialized(pid, seqno, PersistentRepr(Decremented(row.decrementedBy), seqno), None))
      }
  }

  override def countJournal: Future[Int] = db.run(JournalTable.length.result)

  override def highestSequenceNr(persistenceId: String, fromSequenceNr: Long): Future[Long] = {
    val actions = (for {
      seqNumFoundInJournalTable ← JournalTable.filter(_.persistenceId === persistenceId).filter(_.sequenceNumber >= fromSequenceNr).map(_.sequenceNumber).max.result
      highestSeqNumberFoundInDeletedToTable ← JournalDeletedToTable.filter(_.persistenceId === persistenceId).map(_.deletedTo).max.result
      highestSequenceNumber = seqNumFoundInJournalTable.getOrElse(highestSeqNumberFoundInDeletedToTable.getOrElse(0L))
    } yield highestSequenceNumber).transactionally
    db.run(actions)
  }

  override def writeFlow: Flow[Try[Iterable[SerializationResult]], Try[Iterable[SerializationResult]], NotUsed] =
    Flow[Try[Iterable[SerializationResult]]]
      .mapAsync(1) {
        case element @ Success(xs) ⇒ writeList(xs).map(_ ⇒ element)
        case element @ Failure(t)  ⇒ Future.failed(t)
      }

  // only handle non-serialized messages
  override def writeList(xs: Iterable[SerializationResult]): Future[Unit] = {
    println("Writing list: " + this.hashCode())
    val collectPf: PartialFunction[SerializationResult, NotSerialized] = {
      case e: NotSerialized ⇒ e
    }
    // the whole set of SerializationResult, ie. the AtomicWrite must all be
    // persisted in one transaction, or all fail, thus let's process batch wise and
    // drop the streaming strategy.
    val xx = xs.collect(collectPf).map {
      case NotSerialized(pid, seqno, PersistentRepr(event: Incremented, _), tags, created) ⇒
        for {
          _ ← JournalTable += JournalRow(pid, seqno, EventType.Incremented, created, tags)
          _ ← IncrementedTable += IncrementedRow(pid, seqno, event.value)
        } yield ()
      case NotSerialized(pid, seqno, PersistentRepr(event: Decremented, _), tags, created) ⇒
        for {
          _ ← JournalTable += JournalRow(pid, seqno, EventType.Decremented, created, tags)
          _ ← DecrementedTable += DecrementedRow(pid, seqno, event.value)
        } yield ()
    }
    db.run(DBIO.sequence(xx).transactionally).map(_ ⇒ ())
  }
}
