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

import akka.actor.{ Actor, ActorSystem, Props, Terminated }
import akka.event.LoggingReceive
import akka.persistence.PersistentActor
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.PersistenceQuery
import akka.serialization.SerializationExtension
import akka.stream.{ ActorMaterializer, Materializer }
import com.github.dnvriend.domain.PetDomain._
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class Persister(val persistenceId: String)(implicit ec: ExecutionContext) extends PersistentActor {
  val serialization = SerializationExtension(context.system)

  def schedulePersistPet(): Unit =
    context.system.scheduler.scheduleOnce(1.second, self, Pet())

  override def receiveRecover: Receive = LoggingReceive {
    case msg ⇒ println("Recovering: " + msg)
  }

  override def receiveCommand: Receive = persisted(0)

  def persisted(numPets: Int): Receive = LoggingReceive {
    case pet: Pet ⇒
      assert(serialization.findSerializerFor(pet).getClass.getName == "com.twitter.chill.akka.AkkaSerializer")
      // when you look in the database, the length of the message field of the journal should be ~382 bytes
      // Java serialization should be 3x so ~1k
      persist(pet) { _ ⇒
        println("persisted pet: " + numPets)
        context.become(persisted(numPets + 1))
        schedulePersistPet()
      }
  }

  schedulePersistPet()
}

class PersisterSupervisor(persisterProps: Props)(implicit ec: ExecutionContext) extends Actor {
  val createPersister = (props: Props) ⇒ context.actorOf(props)
  val createAndWatchPersister = (props: Props) ⇒ context watch createPersister(props)

  override def preStart(): Unit = {
    createAndWatchPersister(persisterProps)
    super.preStart()
  }

  override def receive: Receive = LoggingReceive {
    case Terminated(ref) ⇒ createAndWatchPersister(persisterProps)
  }
}

object LaunchPet extends App {
  val configName = "pet-application.conf"
  lazy val configuration = ConfigFactory.load(configName)
  implicit val system: ActorSystem = ActorSystem("demo", configuration)
  implicit val mat: Materializer = ActorMaterializer()
  sys.addShutdownHook(system.terminate())
  implicit val ec: ExecutionContext = system.dispatcher
  val readJournal: JdbcReadJournal = PersistenceQuery(system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)
  final val PersistenceId = "persister"

  // async queries :)
  readJournal.eventsByPersistenceId(PersistenceId, 0, Long.MaxValue).runForeach {
    case e ⇒ println(": >>== Received event ==<< : " + e)
  }

  val persisterProps = Props(new Persister(PersistenceId))
  val supervisor = system.actorOf(Props(new PersisterSupervisor(persisterProps)))
}
