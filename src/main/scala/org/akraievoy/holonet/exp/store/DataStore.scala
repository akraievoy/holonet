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

import org.akraievoy.db.Streamable
import java.io._
import scalaz.Lens
import org.akraievoy.cnet.net.vo._
import org.akraievoy.cnet.net.vo.EdgeDataFactory.EdgeDataConstant
import org.akraievoy.holonet.exp.ParamPos

class DataStore(
  val fs: FileSystem,
  val uid: RunUID,
  val chain: Seq[RunUID]
  //  FIXME we should recover live DataStores for chained experiments
) {
  private var scheme: Map[String, String] = Map.empty
  private var openStreams: Map[File, Closeable] = Map.empty
  private var cachedCSV: Map[String, Map[String, Seq[Seq[String]]]] = Map.empty

  def set[T](
    paramName: String,
    value: T,
    spacePos: Seq[ParamPos]
  )(
    implicit mt: Manifest[T]
  ) {
    val pos = ParamPos.pos(spacePos)
    val posStr = java.lang.Long.toString(pos, 16)
    val paramFName = paramKey(paramName, mt)
    if (DataStore.primitives.contains(mt)) {
      synchronized{
        fs.appendCSV(
          uid,
          paramFName,
          openStreams
        )(
          Seq(
            Seq(
              posStr,
              DataStore.primitives(mt).lens.asInstanceOf[Lens[String, T]].set("", value)
            )
          ).toStream
        ).map {
          case (file, closeable) =>
            openStreams = openStreams.updated(file, closeable)
        }
      }
    } else if (DataStore.streamables.contains(mt.asInstanceOf[Manifest[_ <: Streamable]])) {
      val binaryParamFName = "%s\\/%s".format(paramFName, posStr)
        fs.appendBinary(
          uid,
          binaryParamFName,
          openStreams,
          value.isInstanceOf[Store]
        )(
          value.asInstanceOf[Streamable].createStream()
        )
    } else {
      typeNotSupported(mt)
    }
  }

  def get[T](
    paramName: String,
    spacePos: Seq[ParamPos]
  )(
    implicit mt: Manifest[T]
  ): Option[T] = {
    val pos = ParamPos.pos(spacePos)
    val posStr = java.lang.Long.toString(pos, 16)
    val paramFName = paramKey(paramName, mt)

    if (DataStore.primitives.contains(mt)) {
      synchronized {
        cachedCSV.get(paramFName).getOrElse{
          val readCSV = fs.readCSV(uid, paramFName).groupBy{ _.head }
          cachedCSV = cachedCSV.updated(paramFName, readCSV)
          readCSV
        }.getOrElse(posStr, Nil).lastOption.map{
          entry => DataStore.primitives(mt).lens.get(entry.last).asInstanceOf[T]
        }
      }
    } else if (mt <:< manifest[Streamable]) {
      fs.readBinary(
        uid,
        "%s\\/%s".format(paramFName, posStr),
        DataStore.streamables(mt.asInstanceOf[Manifest[T with Streamable]]).readOp
      ).asInstanceOf[Option[T]]
    } else {
      typeNotSupported(mt)
    }
  }

  //  FIXME PROCEED on-completion triggers
  //  FIXME PROCEED listing the parameters per experiment
  //  FIXME PROCEED lookup the parameter in chain

  private def typeNotSupported[T](mt: Manifest[T]): Nothing = {
    throw new IllegalStateException(
      "type not supported: %s".format(mt.erasure)
    )
  }

  private def paramKey(
    paramName: String,
    mt: Manifest[_ <: Any]
  ) = {
    val serializer =
      DataStore.primitives.get(mt).map(_.asInstanceOf[Serializer[_ <: Any]]).orElse(
        DataStore.streamables.get(mt.asInstanceOf[Manifest[_ <: Streamable]])
      ).getOrElse {
        typeNotSupported(mt)
      }
    val alias = serializer.alias

    this.synchronized{
      val aliasStored = scheme.get(paramName).map {
        aliasPrev =>
          if (aliasPrev != alias) {
            throw new IllegalStateException(
              "%s was accessed as %s and now is accessed as %s".format(
                paramName,
                aliasPrev,
                alias
              )
            )
          }
      }.isDefined
      if (!aliasStored) {
        scheme = scheme.updated(paramName, alias)
      }
    }
    "raw/%s--%s".format(paramName, mt.erasure.getSimpleName)
  }

  private def applyOffset[T](
    spacePos: Seq[ParamPos],
    offsets: Map[String, Int]
  ): Seq[ParamPos] = {
    if (offsets.size == 0) {
      spacePos
    } else {
      spacePos.map {
        paramPos =>
          offsets.get(paramPos.name).map {
            offset =>
              paramPos.copy(pos = paramPos.pos + offset)
          }.getOrElse(paramPos)
      }
    }
  }

  def lens[T](
    paramName: String,
    spacePos: Seq[ParamPos],
    offsets: Map[String, Int] = Map.empty
  )(
    implicit mt: Manifest[T]
  ) = {
    val spacePosOffs = applyOffset(spacePos, offsets)

    StoreLens[T](
      {
        () =>
          get[T](paramName, spacePosOffs).get
      }, {
        (t) =>
          set[T](paramName, t, spacePosOffs)
      }
    )
  }

  def readShutdown() {
    cachedCSV = Map.empty
  }

  def writeShutdown() {
    fs.dumpCSV(uid, "chain.csv", Map.empty)(
      chain.toStream.map(
        uid => Seq(uid.expName, uid.confName, uid.stamp)
      )
    )

    fs.dumpCSV(uid, "schema.csv", Map.empty)(
      scheme.map {
        case (pName, pAlias) =>
          Seq(pName, pAlias)
      }.toStream
    )

    openStreams.values.foreach {
      closeable =>
        try {
          closeable match {
            case w: Writer =>
              w.flush()
            case o: OutputStream =>
              o.flush()
            case other =>
            //  do nothing
          }
        } finally {
          closeable.close()
        }
    }

    openStreams = Map.empty
  }
}

object DataStore {
  import java.lang.{
    Byte => JByte, Integer => JInt, Long => JLong,
    Float => JFloat, Double => JDouble
  }

  private lazy val primitives =
    Seq[ValueSerializer[String, _ <: Any]](
      ValueSerializer(
        "String",
        Lens[String, String](
          s => s,
          (s, s1) => s1
        )
      ),
      ValueSerializer(
        "Byte",
        Lens[String, Byte](
          s => JByte.parseByte(s, 16),
          (s, b) => JLong.toString(b, 16)
        )
      ),
      ValueSerializer(
        "Byte",
        Lens[String, JByte](
          s => JByte.parseByte(s, 16),
          (s, b) => JLong.toString(0L + b, 16)
        )
      ),
      ValueSerializer(
        "Int",
        Lens[String, Int](
          s => JInt.parseInt(s, 16),
          (s, i) => JLong.toString(i, 16)
        )
      ),
      ValueSerializer(
        "Int",
        Lens[String, JInt](
          s => JInt.parseInt(s, 16),
          (s, i) => JLong.toString(0L + i, 16)
        )
      ),
      ValueSerializer(
        "Long",
        Lens[String, Long](
          s => JLong.parseLong(s, 16),
          (s, l) => JLong.toString(l, 16)
        )
      ),
      ValueSerializer(
        "Long",
        Lens[String, JLong](
          s => JLong.parseLong(s, 16),
          (s, l) => JLong.toString(l, 16)
        )
      ),
      ValueSerializer(
        "Float",
        Lens[String, Float](
          s => JFloat.intBitsToFloat(JInt.parseInt(s, 16)),
          (s, f) => JLong.toString(JFloat.floatToRawIntBits(f), 16)
        )
      ),
      ValueSerializer(
        "Float",
        Lens[String, JFloat](
          s => JFloat.intBitsToFloat(JInt.parseInt(s, 16)),
          (s, f) => JLong.toString(JFloat.floatToRawIntBits(f), 16)
        )
      ),
      ValueSerializer(
        "Double",
        Lens[String, Double](
          s => JDouble.longBitsToDouble(JLong.parseLong(s, 16)),
          (s, d) => JLong.toString(JDouble.doubleToRawLongBits(d), 16)
        )
      ),
      ValueSerializer(
        "Double",
        Lens[String, JDouble](
          s => JDouble.longBitsToDouble(JLong.parseLong(s, 16)),
          (s, d) => JLong.toString(JDouble.doubleToRawLongBits(d), 16)
        )
      )
    ).groupBy(_.mt).mapValues {
      serSeq => if (serSeq.size > 1) {
        throw new IllegalArgumentException(
          "multiple serializers for %s".format(serSeq.head.mt.erasure.getName)
        )
      }
      serSeq.head
    }

  private lazy val streamables =
    Seq[StreamSerializer[_ <: Streamable]](
      StreamSerializer[VertexData](
        "VertexData",
        input => new VertexData().fromStream(input),
        (vData: VertexData) => vData.createStream()
      ),
      StreamSerializer[EdgeDataDense](
        "EdgeDataDense",
        input => new EdgeDataDense().fromStream(input),
        (eData: EdgeDataDense) => eData.createStream()
      ),
      StreamSerializer[EdgeDataSparse](
        "EdgeDataSparse",
        input => new EdgeDataSparse().fromStream(input),
        (eData: EdgeDataSparse) => eData.createStream()
      ),
      StreamSerializer[EdgeDataConstant](
        "EdgeDataConstant",
        input => new EdgeDataConstant().fromStream(input),
        (eData: EdgeDataConstant) => eData.createStream()
      ),
      StreamSerializer[StoreBit](
        "StoreBit",
        input => new StoreBit().fromStream(input),
        (store: StoreBit) => store.createStream()
      ),
      StreamSerializer[StoreByte](
        "StoreByte",
        input => new StoreByte().fromStream(input),
        (store: StoreByte) => store.createStream()
      )(manifest[StoreByte]),
      StreamSerializer[StoreInt](
        "StoreInt",
        input => new StoreInt().fromStream(input),
        (store: StoreInt) => store.createStream()
      ),
      StreamSerializer[StoreLong](
        "StoreLong",
        input => new StoreLong().fromStream(input),
        (store: StoreLong) => store.createStream()
      ),
      StreamSerializer[StoreFloat](
        "StoreFloat",
        input => new StoreFloat().fromStream(input),
        (store: StoreFloat) => store.createStream()
      ),
      StreamSerializer[StoreDouble](
        "StoreDouble",
        input => new StoreDouble().fromStream(input),
        (store: StoreDouble) => store.createStream()
      )
    ).groupBy(_.mt).mapValues {
      serSeq => if (serSeq.size > 1) {
        throw new IllegalArgumentException(
          "multiple serializers for %s".format(serSeq.head.mt.erasure.getName)
        )
      }
      serSeq.head
    }
}