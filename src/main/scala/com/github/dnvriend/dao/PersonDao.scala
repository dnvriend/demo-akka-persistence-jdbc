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

package com.github.dnvriend.dao

import com.github.dnvriend.dao.PersonDao.PersonEntity
import com.github.dnvriend.dao.PersonTables.PersonTableRow
import slick.jdbc.JdbcBackend

import scala.concurrent.{ExecutionContext, Future}

object PersonDao {
  final case class PersonEntity(id: String, firstname: String, lastname: String, updated: Long)
}

trait PersonDao {
  def savePerson(id: String, firstname: String, lastname: String): Future[Unit]

  def updateFirstname(id: String, firstname: String): Future[Unit]

  def updateLastname(id: String, lastname: String): Future[Unit]

  def persons: Future[Iterable[PersonEntity]]
}

class PersonDaoImpl(db: JdbcBackend#Database)(implicit ec: ExecutionContext) extends PersonDao with PersonTables {
  import akka.persistence.postgres.db.ExtendedPostgresProfile.api._

  override def savePerson(id: String, firstname: String, lastname: String): Future[Unit] =
    db.run(PersonTable += PersonTableRow(id, firstname, lastname, System.currentTimeMillis())).map(_ ⇒ ())

  override def updateFirstname(id: String, firstname: String): Future[Unit] =
    db.run(PersonTable.filter(_.id === id).map(_.firstname).update(firstname)).map(_ ⇒ ())

  override def updateLastname(id: String, lastname: String): Future[Unit] =
    db.run(PersonTable.filter(_.id === id).map(_.lastname).update(lastname)).map(_ ⇒ ())

  override def persons: Future[Iterable[PersonEntity]] =
    db.run(PersonTable.sortBy(_.updated.asc).result)
      .map(xs ⇒ xs.map(row ⇒ PersonEntity(row.id, row.firstname, row.lastname, row.updated)))
}
