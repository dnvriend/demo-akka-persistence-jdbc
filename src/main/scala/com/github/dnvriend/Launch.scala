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

import java.util.UUID

import akka.actor._
import akka.event.LoggingReceive
import akka.persistence.PersistentActor
import akka.persistence.jdbc.query.journal.JdbcReadJournal
import akka.persistence.query.{ EventEnvelope, PersistenceQuery }
import akka.stream.{ ActorMaterializer, Materializer }
import com.github.dnvriend.Person._

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

object Person {

  sealed trait Command

  final case class CreatePerson(firstName: String, lastName: String) extends Command

  final case class ChangeFirstName(firstName: String) extends Command

  final case class ChangeLastName(lastName: String) extends Command

  sealed trait Event

  final case class PersonCreated(firstName: String, lastName: String, timestamp: Long) extends Event

  final case class FirstNameChanged(oldValue: String, newValue: String, timestamp: Long) extends Event

  final case class LastNameChanged(oldValue: String, newValue: String, timestamp: Long) extends Event

  case class PersonState(firstName: String = "", lastName: String = "")

}

class Person(override val persistenceId: String) extends PersistentActor {
  var state = PersonState()

  context.setReceiveTimeout(100.millis)

  def handleEvent(event: Event): Unit = event match {
    case PersonCreated(firstName, lastName, _) ⇒ state = state.copy(firstName = firstName, lastName = lastName)
    case FirstNameChanged(_, newValue, _)      ⇒ state = state.copy(firstName = newValue)
    case LastNameChanged(_, newValue, _)       ⇒ state = state.copy(lastName = newValue)
  }

  override def receiveRecover: Receive = LoggingReceive {
    case event: Event ⇒ handleEvent(event)
  }

  override def receiveCommand: Receive = LoggingReceive {
    case CreatePerson(firstName, lastName) ⇒
      persist(PersonCreated(firstName, lastName, System.currentTimeMillis()))(handleEvent)

    case ChangeFirstName(newValue) ⇒
      persist(FirstNameChanged(state.firstName, newValue, System.currentTimeMillis()))(handleEvent)

    case ChangeLastName(newValue) ⇒
      persist(LastNameChanged(state.lastName, newValue, System.currentTimeMillis()))(handleEvent)

    case ReceiveTimeout ⇒
      context.stop(self)
  }
}

trait ReadJournal {
  _: Actor ⇒
  implicit val ec: ExecutionContext = context.system.dispatcher
  implicit val mat: Materializer = ActorMaterializer()
  val readJournal: JdbcReadJournal = PersistenceQuery(context.system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)
}

object PersonIdRegistry {
  case class NewPersistenceId(id: String)
}

class PersonIdRegistry extends Actor with ActorLogging with ReadJournal {
  import PersonIdRegistry._
  var allPersonIds: Set[String] = Set.empty[String]

  def receiveNewPersistenceId(id: String): Unit =
    self ! NewPersistenceId(id)

  readJournal.allPersistenceIds().runForeach(receiveNewPersistenceId)

  override def receive: Receive = {
    case NewPersistenceId(id) ⇒
      allPersonIds += id
      log.debug("New persistenceId added: {}, size: {}", id, allPersonIds.size)
  }
}

object PersonRegistry {
  case class NewPersistenceId(id: String)
}

class PersonRegistry extends Actor with ActorLogging with ReadJournal {
  import PersonRegistry._
  var allPersons: Map[String, PersonState] = Map.empty

  readJournal.allPersistenceIds().runForeach(receiveNewPersistenceId)

  def receiveNewPersistenceId(id: String): Unit =
    self ! NewPersistenceId(id)

  def handleEvent(event: EventEnvelope): Unit =
    self ! event

  override def receive: Actor.Receive = {
    case NewPersistenceId(id) ⇒
      readJournal.eventsByPersistenceId(id, 0, Long.MaxValue).runForeach(handleEvent)

    case EventEnvelope(_, id, _, event: PersonCreated) if allPersons.isDefinedAt(id) ⇒
      val person = allPersons(id)
      allPersons += (id -> person.copy(firstName = event.firstName, lastName = event.lastName))
      log.debug("Persons: {}", allPersons.size)

    case EventEnvelope(_, id, _, event: PersonCreated) ⇒
      allPersons += (id -> PersonState(event.firstName, event.lastName))
      log.debug("Persons: {}", allPersons.size)
  }
}

class PersonRepository()(implicit val system: ActorSystem) {
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mat: Materializer = ActorMaterializer()
  val readJournal: JdbcReadJournal = PersistenceQuery(system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

  def create(firstName: String, lastName: String): ActorRef = {
    val id = UUID.randomUUID().toString
    val person = find(id)
    person ! CreatePerson(firstName, lastName)
    person
  }

  def find(id: String): ActorRef = system.actorOf(Props(new Person("person#" + id)), id)

  def ids: Future[Set[String]] = readJournal.currentPersistenceIds().filter(_.startsWith("person#")).runFold(List.empty[String])(_ :+ _).map(_.toSet)
}

class SupportDesk extends Actor with ActorLogging with ReadJournal {
  var counter: Long = 0
  val repository = new PersonRepository()(context.system)

  context.system.scheduler.schedule(1.second, 1.second, self, "GO")

  override def receive: Receive = {
    case _ if counter % 2 == 0 ⇒
      repository.create("random", "random") ! ChangeFirstName("FOO")
      counter += 1

    case _ if counter % 3 == 0 ⇒
      repository.create("foo", "bar") ! ChangeLastName("BARR")
      counter += 1

    case _ if counter % 4 == 0 ⇒
      repository.ids.map { ids ⇒
        ids.headOption.foreach { id ⇒
          repository.find(id) ! ChangeLastName("FROM_FOUND")
        }
      }
      counter += 1

    case _ ⇒ counter += 1
  }
}

object Launch extends App {
  implicit val system: ActorSystem = ActorSystem()
  val supportDesk = system.actorOf(Props(new SupportDesk))
  val personIdRegistry = system.actorOf(Props(new PersonIdRegistry))
  val personRegistry = system.actorOf(Props(new PersonRegistry))
}
