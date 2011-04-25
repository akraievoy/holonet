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

package org.akraievoy.cnet.gen.vo;

import org.akraievoy.base.Die;

public abstract class LocationGeneratorBase implements LocationGenerator, Runnable {
  protected int gridSize = 1024;
  protected int cells = gridSize * gridSize;

  public void setGridSize(int gridSize) {
    this.gridSize = gridSize;
    cells = gridSize * gridSize;
  }

  public int getGridSize() {
    return gridSize;
  }

  protected boolean isPos(int nextPos) {
    return 0 <= nextPos && nextPos < cells;
  }

  protected int p2index(final Point p) {
    final double indexDouble = Math.floor(p.getY() * gridSize) * gridSize + Math.floor(p.getX() * gridSize);
    final long indexLong = (long) indexDouble;
    Die.ifFalse("indexLong < cells", indexLong < cells);
    Die.ifFalse("indexLong <= Integer.MAX_VALUE", indexLong <= Integer.MAX_VALUE);

    return (int) indexLong;
  }

  protected Point index2p(int index) {
    final int y = index / gridSize;
    final int x = index % gridSize;

    return new Point(x / (double) gridSize, y / (double) gridSize);
  }

  public abstract Point chooseLocation(EntropySource eSource);

  public abstract double getDensity(Point location);
}
