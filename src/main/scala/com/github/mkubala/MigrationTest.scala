/*
 * Copyright 2020 Marcin Kubala
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

package com.github.mkubala

import akka.actor.{ ActorSystem, Props }
import akka.event.LoggingReceive
import akka.persistence.{ PersistentActor, SnapshotOffer }
import akka.persistence.postgres.migration.journal.JournalMigration
import akka.persistence.postgres.migration.snapshot.SnapshotStoreMigration
import akka.testkit.TestProbe
import com.typesafe.config.{ Config, ConfigFactory }
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.migration.{ BaseJavaMigration, Context }

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }

import pprint._

object MigrationTest {

  lazy val configuration = ConfigFactory.load("migration-application.conf")
  implicit val system: ActorSystem = ActorSystem("MigratedWrapperApp", configuration)

  sys.addShutdownHook(system.terminate())

  implicit val ec: ExecutionContext = system.dispatcher

  def performMigration(): Future[Int] =
    Future {
      val flyway = Flyway.configure()
        .dataSource("jdbc:postgresql://localhost:5432/docker", "docker", "docker")
        .schemas("migration")
        .javaMigrations(new V2_0__MigrateJournal(configuration), new V2_1__MigrateSnapshots(configuration))
        .load()
      flyway.baseline()
      flyway.migrate().migrationsExecuted
    }

  def performAssertions(): Unit = {
    val p = system.actorOf(MigratedPersister.props())
    val tp = TestProbe()

    tp.send(p, "whatever")
    tp.expectMsg(List("b-1", "a-1", "a-1"))
  }

  def main(args: Array[String]): Unit = {
    val program = for {
      successfulMigrations ← performMigration()
      _ ← Future(performAssertions())
      _ ← system.terminate()
    } yield successfulMigrations

    val appliedMigrations = Await.result(program, Duration.Inf)
    log(s"Successfully applied $appliedMigrations migration(s)")
  }
}

class V2_0__MigrateJournal(config: Config)(implicit system: ActorSystem) extends BaseJavaMigration {
  override def migrate(context: Context): Unit = {
    new JournalMigration(config).run()
  }
}

class V2_1__MigrateSnapshots(config: Config)(implicit system: ActorSystem) extends BaseJavaMigration {
  override def migrate(context: Context): Unit = {
    new SnapshotStoreMigration(config).run()
  }
}

class MigratedPersister(val persistenceId: String) extends PersistentActor {

  private var recoveredState: List[String] = Nil

  override def receiveRecover: Receive = {
    case SnapshotOffer(_, snapshot: String) ⇒ recoveredState = recoveredState :+ snapshot
    case evt: String                        ⇒ recoveredState = recoveredState :+ evt
  }

  override def receiveCommand: Receive = LoggingReceive {
    case _ ⇒ sender() ! recoveredState
  }
}

object MigratedPersister {
  def props(persistenceId: String = "foo")(implicit ec: ExecutionContext): Props = Props {
    new MigratedPersister(persistenceId)
  }
}
