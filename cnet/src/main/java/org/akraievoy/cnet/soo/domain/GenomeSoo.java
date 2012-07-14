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

package org.akraievoy.cnet.soo.domain;

import org.akraievoy.base.Die;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;
import org.akraievoy.cnet.opt.api.Genome;

public class GenomeSoo extends Genome {
  protected EdgeData solution;

  public GenomeSoo(EdgeData solution) {
    super(new Object[]{solution});

    this.solution = solution;
  }

  @Deprecated
  //	this no-arg constructor is required
  public GenomeSoo() {
    this(null);
  }

  public EdgeData getSolution() {
    if (solution == null) {
      throw new IllegalArgumentException("solution not set");
    }

    return solution;
  }

  public void setSolution(EdgeData solution) {
    this.solution = solution;
    genomeData[0] = solution;
  }

  public int getLinkNum() {
    return solution.getNotNullCount();
  }

  public Object[] getGenomeData() {
    return genomeData;
  }

  public void setGenomeData(Object[] genome) {
    Die.ifNull("genome", genome);
    Die.ifFalse("genome.length == 1", genome.length == 1);

    setSolution((EdgeData) genome[0]);
  }

  public double similarity(Genome that) {
    return solution.similarity(((GenomeSoo) that).solution);
  }
}
