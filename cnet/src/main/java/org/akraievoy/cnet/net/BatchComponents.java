/*
 Copyright 2011 Anton Kraievoy akraievoy@gmail.com
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

package org.akraievoy.cnet.net;

import org.akraievoy.base.runner.domain.BatchRunner;
import org.akraievoy.base.soft.Soft;
import org.akraievoy.cnet.net.ref.RefEdgeData;
import org.akraievoy.cnet.net.ref.RefVertexData;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.VertexData;

/**
 * Some of operations we might need while batch running: asserting on and printing of data entries.
 *
 * @see BatchRunner
 * @see BatchRunner.ValueProcessor
 */
public class BatchComponents {
  public static class ComponentVDataPrint extends BatchRunner.ComponentValue {
    private final int pos;

    public ComponentVDataPrint(
        String expPath,
        long index,
        String ctxPath,
        int pos
    ) {
      super(expPath, index, ctxPath);
      this.pos = pos;
    }

    public void processValue(Object attr, String attrSpec) {
      if ((attr instanceof RefVertexData)) {
        BatchRunner.log.info(
            "data->value[" + pos + "] = " + (((RefVertexData) attr).getValue()).get(pos) + ": " +
                attrSpec
        );
        return;
      }

      if (!(attr instanceof VertexData)) {
        throw new IllegalStateException(
            "should be a VertexData: " + attrSpec
        );
      }

      BatchRunner.log.info(
          "data[" + pos + "] = " + ((VertexData)attr).get(pos) + ": " +
              attrSpec
      );
    }
  }

  public static class ComponentEDataPrint extends BatchRunner.ComponentValue {
    private final int posFrom;
    private final int posInto;

    public ComponentEDataPrint(
        String expPath,
        long index,
        String ctxPath,
        final int posFrom,
        final int posInto
    ) {
      super(expPath, index, ctxPath);
      this.posFrom = posFrom;
      this.posInto = posInto;
    }

    public void processValue(Object attr, String attrSpec) {
      if ((attr instanceof RefEdgeData)) {
        final RefEdgeData refEData = (RefEdgeData) attr;
        final EdgeData eData = refEData.getValue();
        BatchRunner.log.info(
            "data->value[" + posFrom +"," + posInto + "] = " + eData.get(posFrom, posInto) + ": " +
                attrSpec
        );
        return;
      }

      if (!(attr instanceof EdgeData)) {
        throw new IllegalStateException(
            "should be a VertexData: " + attrSpec
        );
      }

      final EdgeData eData = (EdgeData) attr;
      BatchRunner.log.info(
          "data[" + posFrom +"," + posInto + "] = " + eData.get(posFrom, posInto) + ": " +
              attrSpec
      );
    }
  }

  public static class ComponentVDataEqual extends BatchRunner.ComponentValue {
    private final int pos;
    private final double expected;

    public ComponentVDataEqual(
        String expPath,
        long index,
        String ctxPath,
        final int pos,
        final double expected
    ) {
      super(expPath, index, ctxPath);
      this.pos = pos;
      this.expected = expected;
    }

    public void processValue(Object attr, String attrSpec) {
      if ((attr instanceof RefVertexData)) {
        final double actual = (((RefVertexData) attr).getValue()).get(pos);
        if (!Soft.MICRO.equal(expected, actual)) {
          throw new IllegalStateException(
              "data->value[" + pos + "] = " + actual + ", expected " + expected + ": " +
                  attrSpec
          );
        }
      }

      if (!(attr instanceof VertexData)) {
        throw new IllegalStateException(
            "should be a VertexData: " + attrSpec
        );
      }

      final double actual = ((VertexData) attr).get(pos);
      if (!Soft.MICRO.equal(expected, actual)) {
        throw new IllegalStateException(
            "data[" + pos + "] = " + actual + ", expected " + expected + ": " +
                attrSpec
        );
      }
    }
  }

  public static class ComponentEDataEqual extends BatchRunner.ComponentValue {
    private final int posFrom;
    private final int posInto;
    private final double expected;

    public ComponentEDataEqual(
        String expPath,
        long index,
        String ctxPath,
        final int posFrom,
        final int posInto,
        final double expected
    ) {
      super(expPath, index, ctxPath);
      this.posFrom = posFrom;
      this.posInto = posInto;
      this.expected = expected;
    }

    public void processValue(Object attr, String attrSpec) {
      if ((attr instanceof RefEdgeData)) {
        final RefEdgeData refEData = (RefEdgeData) attr;
        final EdgeData eData = refEData.getValue();
        final double actual = eData.get(posFrom, posInto);
        if (!Soft.MICRO.equal(expected, actual)) {
          throw new IllegalStateException(
              "data->value[" + posFrom + "," + posInto + "] = " + actual + ", expected " + expected + ": " +
                  attrSpec
          );
        }

        return;
      }

      if (!(attr instanceof EdgeData)) {
        throw new IllegalStateException(
            "should be a VertexData: " + attrSpec
        );
      }

      final EdgeData eData = (EdgeData) attr;
      final double actual = eData.get(posFrom, posInto);
      if (!Soft.MICRO.equal(expected, actual)) {
        throw new IllegalStateException(
            "data[" + posFrom + "," + posInto + "] = " + actual + ", expected " + expected + ": " +
                attrSpec
        );
      }
    }
  }
}
