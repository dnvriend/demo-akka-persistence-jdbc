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

package com.github.dnvriend.dao

import com.github.dnvriend.dao.PersonTables.PersonTableRow

object PersonTables {
  final case class PersonTableRow(id: String, firstname: String, lastname: String, updated: Long)
}

trait PersonTables {
  val profile: slick.driver.JdbcProfile

  import profile.api._

  class PersonTable(_tableTag: Tag) extends Table[PersonTableRow](_tableTag, _schemaName = Option("person"), _tableName = "persons") {
    def * = (id, firstname, lastname, updated) <> (PersonTableRow.tupled, PersonTableRow.unapply)

    val id: Rep[String] = column[String]("id", O.Length(255, varying = true))
    val firstname: Rep[String] = column[String]("firstname", O.Length(255, varying = true))
    val lastname: Rep[String] = column[String]("lastname", O.Length(255, varying = true))
    val updated: Rep[Long] = column[Long]("updated")
  }

  lazy val PersonTable = new TableQuery(tag ⇒ new PersonTable(tag))
}
