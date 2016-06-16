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

import akka.actor.{ ActorSystem, PoisonPill, Props }
import akka.event.LoggingReceive
import akka.persistence.{ Persistence, PersistentActor }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.testkit.TestProbe
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext }
import pprint._, Config.Colors.PPrintConfig

object WrapperTest extends App {
  class Persister(val persistenceId: String = "foo") extends PersistentActor {
    val done = (_: Any) ⇒ sender() ! akka.actor.Status.Success("done")
    override def receiveRecover: Receive = akka.actor.Actor.ignoringBehavior
    override def receiveCommand: Receive = LoggingReceive {
      case xs: List[_] ⇒
        log2(xs, "persisting")
        persistAll(xs)(done)
      case "ping" ⇒
        log2("ping => pong", "ping")
        sender() ! "pong"
      case msg: String ⇒
        log2(msg, "persisting")
        persist(msg)(done)
    }
  }

  val configName = "wrapper-application.conf"
  lazy val configuration = ConfigFactory.load(configName)
  implicit val system: ActorSystem = ActorSystem("wrapper", configuration)
  implicit val mat: Materializer = ActorMaterializer()
  sys.addShutdownHook(system.terminate())
  implicit val ec: ExecutionContext = system.dispatcher
  val extension = Persistence(system)

  var p = system.actorOf(Props(new Persister()))
  val tp = TestProbe()
  tp.send(p, (1 to 3).map("a-" + _).toList)
  tp.expectMsg(akka.actor.Status.Success("done"))
  (1 to 3).map("b-" + _).foreach { msg ⇒
    tp.send(p, msg)
    tp.expectMsg(akka.actor.Status.Success("done"))
  }
  tp watch p
  tp.send(p, PoisonPill)
  tp.expectTerminated(p)
  p = system.actorOf(Props(new Persister()))
  tp.send(p, "ping")
  tp.expectMsg("pong")
  Await.ready(system.terminate(), 10.seconds)
}
