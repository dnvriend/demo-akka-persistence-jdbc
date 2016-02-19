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

package com.github.dnvriend

import java.text.SimpleDateFormat
import java.util.{ Date, UUID }

import akka.actor._
import akka.event.LoggingReceive
import akka.persistence.PersistentActor
import akka.persistence.jdbc.query.journal.scaladsl.JdbcReadJournal
import akka.persistence.query.scaladsl._
import akka.persistence.query.{ EventEnvelope, PersistenceQuery }
import akka.stream.{ ActorMaterializer, Materializer }
import com.github.dnvriend.Person.PersonState
import com.github.dnvriend.data.Event.{ PBFirstNameChanged, PBLastNameChanged, PBPersonCreated }
import com.github.dnvriend.domain._

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Random

object Person {
  case class PersonState(firstName: String = "", lastName: String = "")
}

class Person(override val persistenceId: String) extends PersistentActor {
  var state = PersonState()

  context.setReceiveTimeout(100.millis)

  def handleEvent(event: Event): Unit = event match {
    case PersonCreated(firstName, lastName, _) ⇒ state = state.copy(firstName = firstName, lastName = lastName)
    case FirstNameChanged(firstName, _)        ⇒ state = state.copy(firstName = firstName)
    case LastNameChanged(lastName, _)          ⇒ state = state.copy(lastName = lastName)
  }

  override def receiveRecover: Receive = LoggingReceive {
    case event: Event ⇒ handleEvent(event)
  }

  def now: Long = System.currentTimeMillis()

  override def receiveCommand: Receive = LoggingReceive {
    case CreatePerson(firstName, lastName, _) ⇒
      persist(PersonCreated(firstName, lastName, now))(handleEvent)

    case ChangeFirstName(firstName, _) ⇒
      persist(FirstNameChanged(firstName, now))(handleEvent)

    case ChangeLastName(lastName, _) ⇒
      persist(LastNameChanged(lastName, now))(handleEvent)

    case ReceiveTimeout ⇒
      context.stop(self)
  }
}

class PersonRepository(readJournal: ReadJournal with CurrentEventsByTagQuery)(implicit val system: ActorSystem, val mat: Materializer, val ec: ExecutionContext) {
  def now: Long = System.currentTimeMillis()

  def create(firstName: String, lastName: String): ActorRef = {
    val id = UUID.randomUUID().toString
    val person = find(id)
    person ! CreatePerson(firstName, lastName, now)
    person
  }

  def find(id: String): ActorRef = system.actorOf(Props(new Person("person#" + id)), id)

  def countPersons: Future[Long] = readJournal.currentEventsByTag("person-created", 0).runFold(0L) { case (c, _) ⇒ c + 1 }
}

class SupportDesk(repository: PersonRepository, readJournal: ReadJournal with CurrentPersistenceIdsQuery)(implicit val mat: Materializer, ec: ExecutionContext) extends Actor with ActorLogging {
  var counter: Long = 0

  context.system.scheduler.schedule(1.second, 1.second, self, "GO")

  def now: Long = System.currentTimeMillis()

  override def receive: Receive = {
    case _ if counter % 2 == 0 ⇒
      counter += 1
      val rnd = Random.nextInt(2048)
      val actor = repository.create("random" + rnd, "random" + rnd)
      actor ! ChangeFirstName("FOO" + rnd, now)
      actor ! ChangeLastName("BARR" + rnd, now)

    case _ if counter % 3 == 0 ⇒
      counter += 1
      val rnd = Random.nextInt(2048)
      repository.create("foo" + rnd, "bar" + rnd) ! ChangeFirstName("FOO" + rnd, now)
      for {
        count ← repository.countPersons
        num = if (count > Int.MaxValue) Int.MaxValue else count.toInt
        pid ← readJournal.currentPersistenceIds()
          .filter(_.startsWith("person#"))
          .drop(Random.nextInt(num))
          .take(1)
          .runFold("")(_ + _)
        id ← pid.split("person#").drop(1)
      } repository.find(id) ! ChangeLastName("FROM_FOUND", now)

    case _ ⇒
      counter += 1
      println("Nothing to do: " + counter)
  }
}

object Launch extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mat: Materializer = ActorMaterializer()
  val readJournal: ReadJournal with AllPersistenceIdsQuery with EventsByTagQuery with CurrentPersistenceIdsQuery with CurrentEventsByTagQuery = PersistenceQuery(system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)
  val repository = new PersonRepository(readJournal)
  val supportDesk = system.actorOf(Props(new SupportDesk(repository, readJournal)))

  //
  // the read models
  //

  // counts unique pids
  readJournal.allPersistenceIds().runFold(List.empty[String]) {
    case (listOfPids, pid) ⇒
      println(s"New persistenceId received: $pid")
      listOfPids :+ pid
  }

  def format(timestamp: Long): String =
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.XXX").format(new Date(timestamp))

  // counts created persons
  readJournal.eventsByTag("person-created", 0).runFold(0L) {
    case (num, EventEnvelope(_, pid, seqno, PBPersonCreated(firstName, lastName, timestamp))) ⇒
      val total = num + 1
      println(s"Person created $firstName, $lastName on ${format(timestamp)} got id: $pid, total persons: $total")
      total
  }

  // count first name changed
  readJournal.eventsByTag("first-name-changed", 0).runFold(0L) {
    case (num, EventEnvelope(_, pid, seqno, PBFirstNameChanged(newName, timestamp))) ⇒
      val total = num + 1
      println(s"First name changed of pid: $pid to $newName on ${format(timestamp)}, total changed: $total")
      total
  }

  // counts last name changed
  readJournal.eventsByTag("last-name-changed", 0).runFold(0L) {
    case (num, EventEnvelope(_, pid, seqno, PBLastNameChanged(newName, timestamp))) ⇒
      val total = num + 1
      println(s"Last name changed of pid: $pid to $newName on ${format(timestamp)}, total changed: $total")
      total
  }

  val banner = s"""
    |
    |#####  ###### #    #  ####
    |#    # #      ##  ## #    #
    |#    # #####  # ## # #    #
    |#    # #      #    # #    #
    |#    # #      #    # #    #
    |#####  ###### #    #  ####
    |
    |$BuildInfo
    |
  """.stripMargin

  println(banner)
}
