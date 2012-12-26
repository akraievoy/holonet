/*
 Copyright 2012 Anton Kraievoy akraievoy@gmail.com
 This file is part of Holonet.

 Holonet is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Holonet is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Holonet. If not, see <http://www.gnu.org/licenses/>.
 */

package org.akraievoy.holonet.exp.store

import java.io.{InputStream, BufferedInputStream}

case class StreamSerializer[T](
  mt: Manifest[T],
  alias: String,
  readOp: BufferedInputStream => Stream[T],
  writeOp: T => InputStream
) extends Serializer[T]

object StreamSerializer {
  def apply[T](
    alias: String,
    readOp: InputStream => T,
    writeOp: T => InputStream
  )(
    implicit mt: Manifest[T]
  ) = {
    new StreamSerializer(
      mt,
      alias,
      {
        bufferedInput =>
          def readCons: Stream[T] = {
            bufferedInput.mark(8)
            if (bufferedInput.read() != -1) {
              bufferedInput.reset()
              readOp(bufferedInput) #:: readCons
            } else {
              Stream.empty
            }
          }
          readCons
      },
      writeOp
    )
  }
}