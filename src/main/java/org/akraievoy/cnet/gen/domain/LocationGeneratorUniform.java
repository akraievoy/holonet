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

package org.akraievoy.cnet.gen.domain;

import org.akraievoy.base.Die;
import org.akraievoy.cnet.gen.vo.EntropySource;
import org.akraievoy.cnet.gen.vo.LocationGeneratorBase;
import org.akraievoy.cnet.gen.vo.Point;

import java.util.BitSet;

public class LocationGeneratorUniform extends LocationGeneratorBase {
  protected final BitSet usedCells = new BitSet();

  public LocationGeneratorUniform() {
  }

  public void run() {
    usedCells.clear();
  }

  public Point chooseLocation(final EntropySource eSource) {
    Die.ifFalse("freeCellNum > 0", cells > 0);
    final int freeIndex = eSource.nextInt(cells);

    int freeCellCounter = 0;
    for (int index = 0; index < gridSize * gridSize; index++) {
      if (usedCells.get(index)) {
        continue;
      }
      if (freeCellCounter == freeIndex) {
        usedCells.set(index, true);
        cells--;
        return index2p(index);
      }
      freeCellCounter++;
    }

    throw new IllegalStateException("unreachable");
  }

  public double getDensity(Point location) {
    return 1;
  }
}
