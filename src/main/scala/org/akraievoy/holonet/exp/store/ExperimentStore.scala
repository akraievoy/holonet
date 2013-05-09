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
import scalaz.Lens
import org.akraievoy.cnet.net.vo._
import org.akraievoy.cnet.net.vo.EdgeDataFactory.EdgeDataConstant
import org.akraievoy.holonet.exp.{ParamName, Experiment, Config, ParamPos}
import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.BitSet
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConversions.asScalaConcurrentMap
import com.google.common.cache.CacheBuilder

class ExperimentStore(
  val fs: FileSystem,
  val uid: RunUID,
  val experiment: Experiment,
  val config: Config,
  val chain: Seq[ExperimentStore],
  val requiredIndexes: BitSet
) extends CachePimps {

  private val schema: scala.collection.mutable.ConcurrentMap[String, String] =
    new ConcurrentHashMap[String, String](
      64,
      0.125f,
      Runtime.getRuntime.availableProcessors()
    )

  private val openStreamsMonitor = new Object()
  private var openStreams: Map[File, Closeable] = Map.empty

  private val cachedCSVMoninor = new Object()
  private var cachedCSV: Map[String, Map[Long, String]] = Map.empty

  private val cachedBinaries =
    (
      CacheBuilder.newBuilder()
          .concurrencyLevel(Runtime.getRuntime.availableProcessors())
          .maximumSize(512)
          .build[String, Option[Streamable]]()
    )

  private val writeLocked = new AtomicBoolean(false)

  fs.readCSV(uid, "schema.csv").map {
    lineSeq =>
      lineSeq.foreach {
        line =>
          schema.putIfAbsent(line(0), line(1))
      }
  }

  def withChain = chain :+ this

  def set[T](
    paramName: String,
    value: T,
    spacePos: Seq[ParamPos],
    posNum: Long
  )(
    implicit mt: Manifest[T]
  ) {
    if (!mt.erasure.isInstance(value)) {
      throw new IllegalArgumentException(
        "storing to %s value of type %s instead of %s".format(
          paramName, value.getClass, mt.erasure
        )
      )
    }
    spacePos.find {
      paramPos => paramPos.name == paramName
    }.map {
      paramPos =>
        throw new IllegalArgumentException(
          "experiment parameter '%s' value is read-only".format(
            paramName
          )
        )
    }
    chain.find(_.schema.contains(paramName)).map{
      expStore =>
        throw new IllegalStateException(
          "%s is located in chained experiment %s".format(
            paramName,
            expStore.experiment.name
          )
        )
    }

    if (writeLocked.get) {
      throw new IllegalStateException(
        "%s is located in read-only/completed experiment %s".format(
          paramName,
          experiment.name
        )
      )
    }

    val posStr = posNumStr(posNum)
    val paramFName = paramKey(paramName, mt, extendSchema = true).get
    if (ExperimentStore.primitiveSerializers.contains(mt.erasure.getName)) {
        val lens = ExperimentStore.primitiveSerializers(mt.erasure.getName).lens
        val serialized = lens.asInstanceOf[Lens[String, T]].set("", value)
        openStreamsMonitor.synchronized {
          fs.appendCSV(
            uid,
            paramFName,
            openStreams
          )(
            Seq(Seq(posStr, serialized)).toStream
          ).map {
            case (file, closeable) =>
              openStreams = openStreams.updated(file, closeable)
          }
        }
        cachedCSVMoninor.synchronized{
          if (cachedCSV.contains(paramFName)) {
            val cachedParamData = cachedCSV(paramFName)
            cachedCSV = cachedCSV.updated(
              paramFName,
              cachedParamData.updated(
                posNum,
                serialized
              )
            )
          }
        }
    } else if (ExperimentStore.streamableSerializers.contains(mt.erasure.getName)) {
      val binaryParamFName = "%s/%s".format(paramFName, posStr)
      openStreamsMonitor.synchronized {
        fs.appendBinary(
          uid,
          binaryParamFName,
          openStreams,
          value.isInstanceOf[Store]
        )(
          value.asInstanceOf[Streamable].createStream()
        ).map {
          case (file, closeable) =>
            openStreams = openStreams.updated(file, closeable)
        }
      }
    } else {
      typeNotSupported(mt)
    }
  }

  private def posNumStr[T](posNum: Long): String = {
    java.lang.Long.toString(posNum, 16)
  }

  def get[T](
    paramName: String,
    spacePos: Seq[ParamPos],
    posNumbers: Map[String, Long]
  )(
    implicit mt: Manifest[T]
  ): Option[T] = {
    spacePos.find {
      paramPos => paramPos.name == paramName
    }.map {
      paramPos =>
        val lens = ExperimentStore.paramSerializers(paramPos.mt.erasure.getName).lens
        lens.get(paramPos.value).asInstanceOf[T]
    }.orElse {
      val directStore = chain.find {
        prevExp =>
          prevExp.schema.contains(paramName)
      }.getOrElse {
        this
      }
      directStore.getDirect[T](paramName, spacePos, posNumbers(directStore.experiment.name))
    }
  }

  private def getDirect[T](
    paramName: String,
    spacePos: Seq[ParamPos],
    posNum: Long
  )(
    implicit mt: Manifest[T]
  ): Option[T] = {
    paramKey(paramName, mt, extendSchema = false).flatMap {
      paramFName =>
        if (ExperimentStore.primitiveSerializers.contains(mt.erasure.getName)) {
          cachedCSVMoninor.synchronized {
            cachedCSV.get(paramFName).orElse {
              fs.readCSV(uid, paramFName).map {
                lineSeq =>
                  val groupedLines = lineSeq.groupBy {
                    columnSeq => java.lang.Long.parseLong(columnSeq.head, 16)
                  }.mapValues {
                    _.last.last
                  }
                  cachedCSV = cachedCSV.updated(paramFName, groupedLines)
                  groupedLines
              }
            }
          }.flatMap {
            _.get(posNum)
          }.map {
            str =>
              val lens = ExperimentStore.primitiveSerializers(mt.erasure.getName).lens
              lens.get(str).asInstanceOf[T]
          }
        } else if (classOf[Streamable].isAssignableFrom(mt.erasure)) {
          val serializer =
            ExperimentStore.streamableSerializers(
              mt.erasure.getName
            )

          val binaryFName = "" + paramFName + "/" + posNumStr(posNum)
          val fetchOp: (String) => Option[T with Streamable] = {
            binaryFName1 =>
              try {
                fs.readBinary(
                  uid,
                  binaryFName1,
                  serializer.readOp
                ).asInstanceOf[Option[T with Streamable]]
              } catch {
                case e: Exception =>
                  throw new RuntimeException(
                    "failed on read of %s.%s.%s@%s".format(experiment.name, config.name, paramName, posNumStr(posNum)),
                    e
                  )
              }
          }

          if (writeLocked.get) {
            cachedBinaries.getOrLoad(binaryFName)(fetchOp).asInstanceOf[Option[T]]
          } else {
            fetchOp(binaryFName)
          }
        } else {
          typeNotSupported(mt)
        }
    }
  }

  def listParams: Map[String, Class[_]] = {
    val values = schema.toMap.mapValues {
      typeAlias =>
        ExperimentStore.allSerializers.find {
          case (tManifest, serializer) =>
            serializer.alias == typeAlias
        }.get._2.mt.erasure
    }
    values
  }

  private def typeNotSupported[T](mt: Manifest[T]): Nothing = {
    throw new IllegalStateException(
      "type not supported: %s".format(mt.erasure)
    )
  }

  private def paramKey(
    paramName: String,
    mt: Manifest[_ <: Any],
    extendSchema: Boolean
  ): Option[String] = {
    val serializer =
      ExperimentStore.primitiveSerializers.get(mt.erasure.getName).map(
        _.asInstanceOf[Serializer[_ <: Any]]
      ).orElse(
        ExperimentStore.streamableSerializers.get(mt.erasure.getName)
      ).getOrElse {
        typeNotSupported(mt)
      }

    val alias = serializer.alias

    val aliasStored = (
      if (extendSchema) {
        if (writeLocked.get) {
          throw new IllegalStateException("attempting to add param %s after write shutdown".format(paramName))
        }
        schema.putIfAbsent(paramName, alias)
      } else {
        schema.get(paramName)
      }
    ).map {
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

    if (aliasStored || extendSchema) {
      Some("raw/" + paramName + "--" + alias)
    } else {
      None
    }
  }

  def readShutdown() {
    cachedCSVMoninor.synchronized{
      cachedCSV = Map.empty
    }
  }

  def writeShutdown() {
    writeLocked.set(true)

    fs.dumpCSV(uid, "chain.csv", Map.empty)(
      chain.toStream.map(
        store => Seq(store.uid.expName, store.uid.confName, store.uid.stamp)
      )
    )

    fs.dumpCSV(uid, "schema.csv", Map.empty)(
      schema.map {
        case (pName, pAlias) =>
          Seq(pName, pAlias)
      }.toStream
    )

    openStreamsMonitor.synchronized {
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

  def primitives: Seq[ParamName[_]] = {
    schema.filter {
      case (name, alias) =>
        ExperimentStore.primitiveAliases.contains(alias)
    }.toSeq.map{
      case (name, alias) =>
        new ParamName(
          ExperimentStore.primitiveAliasToSerializer(alias).mt.asInstanceOf[Manifest[_]],
          name
        )
    }
  }
}

object ExperimentStore {
  import java.lang.{
    Byte => JByte, Integer => JInt, Long => JLong,
    Float => JFloat, Double => JDouble
  }

  protected lazy val primitiveSerializers: Map[String, ValueSerializer[String, _]] =
    Seq[ValueSerializer[String, _]](
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
    ).groupBy(_.mt.erasure.getName).mapValues {
      serSeq => if (serSeq.size > 1) {
        throw new IllegalArgumentException(
          "multiple serializers for %s".format(serSeq.head.mt.erasure.getName)
        )
      }
      serSeq.head
    }

  protected lazy val primitiveAliases =
    primitiveSerializers.values.map{
      ser => ser.alias
    }.toSet

  protected lazy val primitiveAliasToSerializer =
    primitiveAliases.map{
      alias =>
        (
            alias ->
            primitiveSerializers.values.find{
              serializer =>
                serializer.alias == alias &&
                  serializer.mt <:< manifest[AnyRef]
            }.get
        )
    }.toMap[String, ValueSerializer[String, _]]

  protected lazy val paramSerializers: Map[String, ValueSerializer[String, _]] =
    Seq[ValueSerializer[String, _]](
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
          s => JByte.parseByte(s),
          (s, b) => JLong.toString(b)
        )
      ),
      ValueSerializer(
        "Byte",
        Lens[String, JByte](
          s => JByte.parseByte(s),
          (s, b) => JLong.toString(0L + b)
        )
      ),
      ValueSerializer(
        "Int",
        Lens[String, Int](
          s => JInt.parseInt(s),
          (s, i) => JLong.toString(i)
        )
      ),
      ValueSerializer(
        "Int",
        Lens[String, JInt](
          s => JInt.parseInt(s),
          (s, i) => JLong.toString(0L + i)
        )
      ),
      ValueSerializer(
        "Long",
        Lens[String, Long](
          s => JLong.parseLong(s),
          (s, l) => JLong.toString(l)
        )
      ),
      ValueSerializer(
        "Long",
        Lens[String, JLong](
          s => JLong.parseLong(s),
          (s, l) => JLong.toString(l)
        )
      ),
      ValueSerializer(
        "Float",
        Lens[String, Float](
          s => JFloat.parseFloat(s),
          (s, f) => JFloat.toString(f)
        )
      ),
      ValueSerializer(
        "Float",
        Lens[String, JFloat](
          s => JFloat.parseFloat(s),
          (s, f) => JFloat.toString(f)
        )
      ),
      ValueSerializer(
        "Double",
        Lens[String, Double](
          s => JDouble.parseDouble(s),
          (s, d) => JDouble.toString(d)
        )
      ),
      ValueSerializer(
        "Double",
        Lens[String, JDouble](
          s => JDouble.parseDouble(s),
          (s, d) => JDouble.toHexString(d)
        )
      )
    ).groupBy(_.mt.erasure.getName).mapValues {
      serSeq => if (serSeq.size > 1) {
        throw new IllegalArgumentException(
          "multiple serializers for %s".format(serSeq.head.mt.erasure.getName)
        )
      }
      serSeq.head
    }

  protected lazy val streamableSerializers: Map[String, StreamSerializer[_ <: Streamable]] =
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
    ).groupBy(_.mt.erasure.getName).mapValues {
      serSeq => if (serSeq.size > 1) {
        throw new IllegalArgumentException(
          "multiple serializers for %s".format(serSeq.head.mt.erasure.getName)
        )
      }
      serSeq.head
    }

  protected lazy val allSerializers: Map[String, Serializer[Any]] = {
    primitiveSerializers.withDefault(streamableSerializers.map{
      case (m, s) =>
        (
          m.asInstanceOf[Manifest[_]],
          s
        )
    }.toMap)
  }
}