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

import java.util.concurrent.atomic.AtomicLong
import annotation.tailrec
import java.text.SimpleDateFormat
import java.util.Date

class RunStore(
  val fs: FileSystem
) {
  private val stampGenerator =
    new AtomicLong(System.currentTimeMillis())

  private val dateFormat =
    new SimpleDateFormat("yyMMdd-HHmmSS")

  @tailrec
  private def generateStamp: Long = {
    val prevValue = stampGenerator.get()
    val newValue = System.currentTimeMillis()
    if (newValue - prevValue < 1000) {
      Thread.sleep(1000)
      generateStamp
    } else if (stampGenerator.compareAndSet(prevValue, newValue)) {
      newValue
    } else {
      generateStamp
    }
  }

  def registerRun(
    expName: String,
    confName: String
  ): RunUID = {
    RunUID(
      expName,
      confName,
      dateFormat.format(new Date(generateStamp))
    )
  }
}
