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

import akka.actor.ActorSystem
import akka.persistence.postgres.migration.AkkaPersistencePostgresMigration
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }

object MigrationTest extends App {

  lazy val configuration = ConfigFactory.load("migration-application.conf")
  implicit val system: ActorSystem = ActorSystem("MigratedWrapperApp", configuration)

  sys.addShutdownHook(system.terminate())

  implicit val ec: ExecutionContext = system.dispatcher

  def performMigration(): Future[Int] =
    AkkaPersistencePostgresMigration.configure(configuration)
      .withMigrationLogTableSchema("migration")
      .build
      .run

  val program = for {
    successfulMigrations ← performMigration()
    _ ← Future(WrapperTest.run())
    _ ← system.terminate()
  } yield successfulMigrations

  val appliedMigrations = Await.result(program, Duration.Inf)
  println(s"Successfully applied $appliedMigrations migration(s)")
}
