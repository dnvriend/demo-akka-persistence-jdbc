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

package com.github.dnvriend.domain

sealed trait Command

final case class CreatePerson(firstName: String, lastName: String, timestamp: Long) extends Command

final case class ChangeFirstName(firstName: String, timestamp: Long) extends Command

final case class ChangeLastName(lastName: String, timestamp: Long) extends Command

sealed trait Event

final case class PersonCreated(firstName: String, lastName: String, timestamp: Long) extends Event

final case class FirstNameChanged(firstName: String, timestamp: Long) extends Event

final case class LastNameChanged(lastName: String, timestamp: Long) extends Event

