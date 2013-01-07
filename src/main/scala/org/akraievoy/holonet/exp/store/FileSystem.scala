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

import java.io._
import com.google.common.io.ByteStreams

class FileSystem(
  baseDir: File
) {

  if (!baseDir.isDirectory) {
    throw new IllegalStateException(
      "baseDir '%s' does not exist".format(baseDir.getAbsolutePath)
    )
  }

  def appendCSV(
    runUID: RunUID,
    fName: String,
    openStreams: Map[File, Closeable]
  )(
    data: Stream[Seq[String]]
  ): Option[(File, PrintWriter)] = {
    val destFile = new File(expDir(runUID), fName)
    val (w, opened) = openStreams.get(destFile).map{
      closeable => (closeable.asInstanceOf[PrintWriter], false)
    }.getOrElse{
      destFile.getParentFile.mkdirs()
      (new PrintWriter(new FileWriter(destFile, true)), true)
    }

    data.foreach(rowSeq => w.println(rowSeq.mkString(";")))
    w.flush()

    if (opened) {
      Some((destFile, w))
    } else {
      None
    }
  }

  def readCSV(
    runUID: RunUID,
    fName: String
  ): Option[Seq[Seq[String]]] = {
    val destFile = new File(expDir(runUID), fName)
    if (destFile.isFile) {
      val in = new FileInputStream(destFile)
      try {
        val lineSeq = io.Source.fromInputStream(in, "UTF-8").getLines().map {
          str => str.split(";").map {
            cell => cell.trim
          }.toSeq
        }.toIndexedSeq  //  toSeq does not force stream materialization, hmmm
        Some(lineSeq)
      } finally {
        in.close()
      }
    } else {
      None
    }
  }

  def dumpCSV(
    runUID: RunUID,
    fName: String,
    openStreams: Map[File, Closeable]
  )(
    data: Stream[Seq[String]]
  ) {
    val destFile = new File(expDir(runUID), fName)

    noReadsAndDumpsOverAppends(openStreams, destFile)

    destFile.getParentFile.mkdirs()

    val w = new PrintWriter(new FileWriter(destFile, false))
    data.foreach(rowSeq => w.println(rowSeq.mkString(";")))
    w.flush()
    w.close()
  }

  def readBinary[T](
    runUID: RunUID,
    fName: String,
    readOp: BufferedInputStream => Stream[T],
    openStreams: Map[File, Closeable] = Map.empty
  ): Option[T] = {
    val srcFile = new File(expDir(runUID), fName)

    noReadsAndDumpsOverAppends(openStreams, srcFile)
    if (srcFile.isFile) {
      val in = new BufferedInputStream(new FileInputStream(srcFile))
      try {
        readOp(in).lastOption
      } finally {
        in.close()
      }
    } else {
      None
    }
  }

  def appendBinary(
    runUID: RunUID,
    fName: String,
    openStreams: Map[File, Closeable],
    forceKeepOpen: Boolean = false
  )(in: InputStream): Option[(File, FileOutputStream)] = {
    val destFile = new File(expDir(runUID), fName)
    val (out, opened) = openStreams.get(destFile).map {
      closeable => (closeable.asInstanceOf[FileOutputStream], false)
    }.getOrElse {
      destFile.getParentFile.mkdirs()
      (new FileOutputStream(destFile, true), true)
    }

    var res: Option[(File, FileOutputStream)] = None

    try {
      ByteStreams.copy(in, out)
      out.flush()
    } finally {
      if (!forceKeepOpen) {
        out.close()
        res = None
      } else {
        if (opened) {
          res = Some((destFile, out))
        } else {
          None
        }
      }
    }

    res
  }

  private def noReadsAndDumpsOverAppends(
    openStreams: Map[File, Closeable],
    destFile: File
  ) {
    if (openStreams.contains(destFile)) {
      throw new IllegalStateException(
        "reading from/dumping to previously appended file: '%s'".format(
          destFile.getAbsolutePath
        )
      )
    }
  }

  def expDir(runUID: RunUID): File = {
    new File(baseDir, runUID.dirName)
  }
}
