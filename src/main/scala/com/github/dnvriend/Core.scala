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

import akka.actor.ActorSystem
import akka.persistence.jdbc.query.journal.scaladsl.JdbcReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.{ ActorMaterializer, Materializer }
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext

trait Core {
  def resourceName: String
  implicit def system: ActorSystem = ActorSystem("demo", ConfigFactory.load(resourceName))
  implicit def ec: ExecutionContext = system.dispatcher
  implicit def mat: Materializer = ActorMaterializer()
  def readJournal: JdbcReadJournal = PersistenceQuery(system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)
}
