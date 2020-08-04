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

import java.util.Base64

import com.github.tminglei.slickpg.utils.PlainSQLUtils
import com.github.tminglei.slickpg.utils.PlainSQLUtils.mkGetResult
import slick.jdbc.{ GetResult, SetParameter }

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext }

trait DbCleanup {

  import akka.persistence.postgres.db.ExtendedPostgresProfile.api._

  implicit def ec: ExecutionContext

  def db: Database

  def truncateJournal: DBIOAction[Unit, NoStream, Effect] = sqlu"truncate table journal".map(_ ⇒ ())
}

object DatabaseTest extends App with DbCleanup {

  import akka.persistence.postgres.db.ExtendedPostgresProfile.api._

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val db: Database = Database.forURL(url = "jdbc:postgresql://localhost:5432/docker", user = "docker", password = "docker", driver = "org.postgresql.Driver")

  implicit val getByteArray: GetResult[Array[Byte]] = mkGetResult[Array[Byte]](_.nextBytes())
  implicit val setByteArray: SetParameter[Array[Byte]] = PlainSQLUtils.mkSetParameter[Array[Byte]]("bytea", Base64.getEncoder.encodeToString)

  type JournalRow = (String, Long, Long, Seq[Int], Array[Byte])
  implicit val getRowResult: GetResult[JournalRow] = mkGetResult[JournalRow](r ⇒ (r.<<, r.<<, r.<<, r.<<, r.<<[Array[Byte]]))

  // insert some records
  val insertEvents = DBIO.sequence(List(
    ("pid1", 1, Seq(1, 2, 3, 4), "Dog"),
    ("pid1", 2, Seq(1, 2, 3, 4), "Cat"),
    ("pid1", 3, Seq(1, 2, 3, 4), "Fish")).map {
      case (pid, seqno, tags, message) ⇒
        sqlu"INSERT INTO journal (persistence_id, sequence_number, tags, message) VALUES ($pid, $seqno, $tags, ${message.getBytes("UTF-8")})"
    }).transactionally

  val queryEvents = sql"SELECT persistence_id, sequence_number, ordering, tags, message FROM journal".as[JournalRow]

  val dbActions = for {
    _ ← truncateJournal
    _ ← insertEvents
    r ← queryEvents
  } yield r.map {
    case (pid, seq, ord, tags, msg) ⇒
      val decodedMsg = new String(Base64.getDecoder.decode(msg), "UTF-8")
      (pid, seq, ord, tags.mkString("(", ", ", ")"), decodedMsg)
  }.toList

  val rows = db.run(dbActions)

  println(formatTable(Await.result(rows, 1.second)))

  // An utility method to prettify the results
  private def formatTable(dbRows: List[(String, Long, Long, String, String)]): String = {
    if (dbRows.isEmpty) "--- Empty Rows Set ---"
    else {
      val header = List("persistence_id", "seuence_number", "ordering", "tags", "message")
      val table = header :: dbRows.map(_.productIterator.toList)
      // Get column widths based on the maximum cell width in each column (+2 for a one character padding on each side)
      val colWidths = table.transpose.map(_.map(cell ⇒ if (cell == null) 0 else cell.toString.length).max + 2)
      // Format each row
      val rows = table.map(_.zip(colWidths).map { case (item, size) ⇒ (" %-" + (size - 1) + "s").format(item) }
        .mkString("|", "|", "|"))
      // Formatted separator row, used to separate the header and draw table borders
      val separator = colWidths.map("-" * _).mkString("+", "+", "+")
      // Put the table together and return
      (separator +: rows.head +: separator +: rows.tail :+ separator).mkString("\n")
    }
  }
}
