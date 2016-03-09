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
import akka.persistence.jdbc.dao.JournalDao
import akka.persistence.jdbc.serialization.SerializationResult
import akka.stream.scaladsl.{ Flow, Source }
import slick.driver.JdbcProfile
import slick.jdbc.JdbcBackend

import scala.concurrent.Future
import scala.util.Try

class CounterJournalDao(db: JdbcBackend#Database, val profile: JdbcProfile, system: ActorSystem) extends JournalDao {
  override def allPersistenceIdsSource: Source[String, NotUsed] = ???

  override def writeFlow: Flow[Try[Iterable[SerializationResult]], Try[Iterable[SerializationResult]], NotUsed] = ???

  override def eventsByPersistenceIdAndTag(persistenceId: String, tag: String, offset: Long): Source[SerializationResult, NotUsed] = ???

  override def countJournal: Future[Int] = ???

  override def highestSequenceNr(persistenceId: String, fromSequenceNr: Long): Future[Long] = ???

  override def eventsByTag(tag: String, offset: Long): Source[SerializationResult, NotUsed] = ???

  override def persistenceIds(queryListOfPersistenceIds: Iterable[String]): Future[Seq[String]] = ???

  override def writeList(xs: Iterable[SerializationResult]): Future[Unit] = ???

  override def delete(persistenceId: String, toSequenceNr: Long): Future[Unit] = ???

  override def messages(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long, max: Long): Source[SerializationResult, NotUsed] = ???
}
