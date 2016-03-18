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
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import akka.event.LoggingReceive
import akka.persistence.PersistentActor
import akka.persistence.jdbc.query.journal.scaladsl.JdbcReadJournal
import akka.persistence.query.scaladsl._
import akka.persistence.query.{ EventEnvelope, PersistenceQuery }
import akka.stream.{ ActorMaterializer, Materializer }
import com.github.dnvriend.data.Event.{ PBFirstNameChanged, PBLastNameChanged, PBPersonCreated }
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Random

object Person {

  sealed trait Command

  final case class CreatePerson(firstName: String, lastName: String, timestamp: Long) extends Command

  final case class ChangeFirstName(firstName: String, timestamp: Long) extends Command

  final case class ChangeLastName(lastName: String, timestamp: Long) extends Command

  // events
  sealed trait Event
  final case class PersonCreated(firstName: String, lastName: String, timestamp: Long) extends Event
  final case class FirstNameChanged(firstName: String, timestamp: Long) extends Event
  final case class LastNameChanged(lastName: String, timestamp: Long) extends Event

  // the state
  final case class PersonState(firstName: String = "", lastName: String = "")

  // necessary for cluster sharding
  final case class EntityEnvelope(id: String, payload: Any)

  final val NumberOfShards: Int = 100

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case EntityEnvelope(id, payload) ⇒ (id.toString, payload)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case EntityEnvelope(id, _) ⇒ (id.hashCode % NumberOfShards).toString
  }

  final val PersonShardName = "Person"
}

class Person extends PersistentActor with ActorLogging {
  import Person._
  import ShardRegion.Passivate

  override val persistenceId: String = "Person-" + self.path.name

  context.setReceiveTimeout(300.millis)

  var state = PersonState()

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
    case CreatePerson(firstName, lastName, _) ⇒ persist(PersonCreated(firstName, lastName, now))(handleEvent)
    case ChangeFirstName(firstName, _)        ⇒ persist(FirstNameChanged(firstName, now))(handleEvent)
    case ChangeLastName(lastName, _)          ⇒ persist(LastNameChanged(lastName, now))(handleEvent)
    case ReceiveTimeout                       ⇒ context.parent ! Passivate(stopMessage = SupervisorStrategy.Stop)
    case SupervisorStrategy.Stop              ⇒ context.stop(self)
  }
}

object SupportDesk {
  final case class ChangeFirstName(id: String)
  final case class ChangeLastName(id: String)
}

class SupportDesk(personRegion: ActorRef, readJournal: ReadJournal with CurrentPersistenceIdsQuery)(implicit val mat: Materializer, ec: ExecutionContext) extends Actor with ActorLogging {
  import Person._
  private var counter: Int = 0

  context.system.scheduler.schedule(1.second, 1.second, self, "GO")

  def now: Long = System.currentTimeMillis()

  override def receive: Receive = {
    case _ if counter % 2 == 0 ⇒
      val id = UUID.randomUUID.toString
      personRegion ! EntityEnvelope(id, CreatePerson("FOO", "BAR", now))
      context.system.scheduler.scheduleOnce(5.seconds, self, SupportDesk.ChangeFirstName(id))
      context.system.scheduler.scheduleOnce(10.seconds, self, SupportDesk.ChangeLastName(id))
      counter += 1

    case SupportDesk.ChangeFirstName(id) ⇒
      personRegion ! EntityEnvelope(id, ChangeFirstName(s"FOO-${DateUtil.format(now)}", now))

    case SupportDesk.ChangeLastName(id) ⇒
      personRegion ! EntityEnvelope(id, ChangeLastName(s"BAR-${DateUtil.format(now)}", now))

    case _ ⇒
      counter += 1
      println("Nothing to do: " + counter)
  }
}

object DateUtil {
  def format(timestamp: Long): String =
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.XXX").format(new Date(timestamp))
}

object LaunchPerson extends App {
  val configName = "person-application.conf"
  lazy val configuration = ConfigFactory.load(configName)
  implicit val system: ActorSystem = ActorSystem("ClusterSystem", configuration)
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mat: Materializer = ActorMaterializer()
  lazy val readJournal: JdbcReadJournal = PersistenceQuery(system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

  // launch the personShardRegion; the returned actor must be used to send messages to the shard
  val personRegion: ActorRef = ClusterSharding(system).start(
    typeName = Person.PersonShardName,
    entityProps = Props[Person],
    settings = ClusterShardingSettings(system),
    extractEntityId = Person.extractEntityId,
    extractShardId = Person.extractShardId
  )

  val supportDesk = system.actorOf(Props(new SupportDesk(personRegion, readJournal)))

  //
  // the read models
  //

  // counts unique pids
  readJournal.allPersistenceIds().runFold(List.empty[String]) {
    case (listOfPids, pid) ⇒
      println(s"New persistenceId received: $pid")
      listOfPids :+ pid
  }

  // counts created persons
  readJournal.eventsByTag("person-created", 0).runFold(0L) {
    case (num, EventEnvelope(_, pid, seqno, PBPersonCreated(firstName, lastName, timestamp))) ⇒
      val total = num + 1
      println(s"Person created $firstName, $lastName on ${DateUtil.format(timestamp)} got id: $pid/$seqno, total persons: $total")
      total
  }

  // count first name changed
  readJournal.eventsByTag("first-name-changed", 0).runFold(0L) {
    case (num, EventEnvelope(_, pid, seqno, PBFirstNameChanged(newName, timestamp))) ⇒
      val total = num + 1
      println(s"First name changed of pid/seqno: $pid/$seqno to $newName on ${DateUtil.format(timestamp)}, total changed: $total")
      total
  }

  // counts last name changed
  readJournal.eventsByTag("last-name-changed", 0).runFold(0L) {
    case (num, EventEnvelope(_, pid, seqno, PBLastNameChanged(newName, timestamp))) ⇒
      val total = num + 1
      println(s"Last name changed of pid/seqno: $pid/$seqno to $newName on ${DateUtil.format(timestamp)}, total changed: $total")
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
