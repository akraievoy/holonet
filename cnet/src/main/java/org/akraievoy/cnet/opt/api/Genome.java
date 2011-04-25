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

package org.akraievoy.cnet.opt.api;

import java.util.Arrays;

/**
 * To maintain easy access solution data, Genome should consist of some number of indexed&typed components.
 * For example, some algo may use two EdgeDatas, one VertexData and a double which then might be analyzed separately.
 * <p/>
 * This type is not written to db via simple Jackson serialization, each component is typed and stored separately (which allows for the goals depicted above).
 */
public abstract class Genome {
  protected Object[] genomeData;

  public Genome() {
  }

  public Genome(Object[] genomeData) {
    this.genomeData = genomeData;
  }

  public Object[] getGenomeData() {
    return genomeData;
  }

  public void setGenomeData(Object[] genomeData) {
    this.genomeData = genomeData;
  }

  public boolean isDupeOf(Genome that) {
    return Arrays.deepEquals(genomeData, that.genomeData);
  }

  public abstract double similarity(Genome that);
}
