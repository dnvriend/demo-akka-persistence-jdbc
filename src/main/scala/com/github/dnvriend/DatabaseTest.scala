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

import java.sql.{ ResultSet, Statement }

import slick.driver.PostgresDriver.profile.api._

import scala.concurrent.Await
import scala.concurrent.duration._

object DatabaseTest extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  val db = Database.forURL(url = "jdbc:postgresql://boot2docker:5432/docker", user = "docker", password = "docker", driver = "org.postgresql.Driver")
  Await.ready(db.run(sql"select persistence_id from journal".as[String]).map(xs ⇒ println(xs)), 1.second)

  // going synchronous!

  def withDatabase[A](f: Database ⇒ A): A = f(db)

  def withSession[A](f: Session ⇒ A): A = withDatabase { db ⇒
    val session = db.createSession()
    try f(session) finally session.close()
  }

  def withStatement[A](f: Statement ⇒ A): A =
    withSession(session ⇒ session.withStatement()(f))

  def tableColumnCount(table: String): Option[Int] = withStatement { stmt ⇒
    val selectNumColumnsQuery: String = s"SELECT COUNT(*) from INFORMATION_SCHEMA.COLUMNS WHERE table_schema = 'public' and table_name = '$table'"
    def loop(rs: ResultSet): Option[Int] = if (rs.next) Option(rs.getInt(1)) else None
    loop(stmt.executeQuery(selectNumColumnsQuery))
  }

  def getTable(table: String): List[List[String]] = {
    def getTable(numFields: Int): List[List[String]] = withStatement { stmt ⇒
      def loopFields(rs: ResultSet, fieldNum: Int, acc: List[String]): List[String] =
        if (fieldNum == numFields + 1) acc else loopFields(rs, fieldNum + 1, acc :+ rs.getString(fieldNum))
      def loop(rs: ResultSet, acc: List[List[String]]): List[List[String]] =
        if (rs.next) loop(rs, acc :+ loopFields(rs, 1, Nil)) else acc
      loop(stmt.executeQuery(s"SELECT * FROM $table"), Nil)
    }
    getTable(tableColumnCount(table).get)
  }

  // insert some records
  val journalEntries = List(
    ("pid1", 1, 10000, "1,2,3,4", "DEADBEAF"),
    ("pid1", 2, 10001, "1,2,3,4", "DEADBEAF"),
    ("pid1", 3, 10002, "1,2,3,4", "DEADBEAF")
  ).map {
      case (pid, seqno, created, tags, message) ⇒
        s"INSERT INTO journal (persistence_id, sequence_number, created, tags, message) VALUES ('$pid', $seqno, $created, '$tags', decode('$message', 'hex'))"
    }
  withStatement(stmt ⇒ journalEntries.foreach(stmt.executeUpdate))

  getTable("journal").foreach(println)
  println("exit")
}
