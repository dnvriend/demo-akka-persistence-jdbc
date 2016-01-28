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

package com.github.dnvriend.adapter

import akka.persistence.journal.{ Tagged, WriteEventAdapter }
import com.github.dnvriend.domain.Person._

class TaggingEventAdapter extends WriteEventAdapter {
  override def manifest(event: Any): String = ""

  def withTag(event: Any, tag: String) = Tagged(event, Set(tag))

  override def toJournal(event: Any): Any = event match {
    case _: PersonCreated ⇒
      withTag(event, "person-created")
    case _: FirstNameChanged ⇒
      withTag(event, "first-name-changed")
    case _: LastNameChanged ⇒
      withTag(event, "last-name-changed")
    case _ ⇒ event
  }
}
