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

package com.github.mkubala.serializer

import akka.serialization.SerializerWithStringManifest
import com.github.mkubala.Person.ChangeFirstName
import com.github.mkubala.data.Command.PBChangeFirstName

/**
 * Converts FirstName Google Protobuf Message
 * to byte array and back
 */
class ChangeFirstNameSerializer extends SerializerWithStringManifest {

  override def identifier: Int = 100

  final val Manifest = classOf[ChangeFirstName].getName

  override def manifest(o: AnyRef): String = o.getClass.getName

  /**
   * Unmarshal to the data model
   */
  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    if (Manifest == manifest) {
      val PBChangeFirstName(firstName, timestamp) = PBChangeFirstName.parseFrom(bytes)
      ChangeFirstName(firstName, timestamp)
    } else throw new IllegalArgumentException("Unable to handle manifest: " + manifest)

  /**
   * Marshal the data model to bytes
   */
  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case ChangeFirstName(firstName, timestamp) ⇒ PBChangeFirstName(firstName, timestamp).toByteArray
    case _                                     ⇒ throw new IllegalStateException("Cannot serialize: " + o.getClass.getName)
  }
}
