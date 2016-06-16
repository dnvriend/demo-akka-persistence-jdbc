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

package com.github.dnvriend.journal

import akka.persistence.PersistentRepr
import akka.persistence.jdbc.journal.JdbcAsyncWriteJournal
import akka.persistence.jdbc.serialization.SerializationResult
import akka.stream.FlowShape
import akka.stream.scaladsl.{ Broadcast, GraphDSL, Zip }
import com.github.dnvriend.adapter.Wrapper
import com.typesafe.config.Config

import scala.concurrent.Future

class WrapperJdbcAsyncWriteJournal(config: Config) extends JdbcAsyncWriteJournal(config) {

  val serGraph = GraphDSL.create() { implicit builder ⇒
    import GraphDSL.Implicits._
    val broadcast = builder.add(Broadcast[SerializationResult](2))
    val zip = builder.add(Zip[PersistentRepr, Long]())
    broadcast.out(0) ~> serializationFacade.deserializeRepr.mapAsync(1)(deserializedRepr ⇒ Future.fromTry(deserializedRepr)) ~> zip.in0
    broadcast.out(1).map(ser ⇒ ser.created) ~> zip.in1
    FlowShape(broadcast.in, zip.out)
  }

  override def asyncReplayMessages(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long, max: Long)(recoveryCallback: (PersistentRepr) ⇒ Unit): Future[Unit] = {
    journalDao.messages(persistenceId, fromSequenceNr, toSequenceNr, max)
      .via(serGraph)
      .map { case (repr, created) ⇒ repr.withPayload(Wrapper(repr.payload, created)) }
      .runForeach(recoveryCallback)
      .map(_ ⇒ ())
  }
}
