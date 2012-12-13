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

package org.akraievoy.cnet.net.vo;

public class IndexCodec {
  public static final int POW = 14;
  public static final int MASK = (1 << POW) - 1;

  protected boolean intoLeads;

  @Deprecated
  public IndexCodec() {
  }

  public IndexCodec(final boolean intoLeads) {
    this.intoLeads = intoLeads;
  }

  public int fi2id(final int from, final int into) {
    if (intoLeads) {
      return (into << POW) + from;
    }

    return (from << POW) + into;
  }

  public int lt2id(final int leading, final int trailing) {
    return (leading << POW) + trailing;
  }

  public int leading2minId(final int leading) {
    return leading << POW;
  }

  public int leading2maxId(final int leading) {
    return (leading + 1) << POW;
  }

  public int id2trailing(final int id) {
    return id & MASK;
  }

  public int id2leading(final int id) {
    return id >> POW;
  }

  @SuppressWarnings({"UnusedDeclaration"})
  @Deprecated
  public boolean isIntoLeads() {
    return intoLeads;
  }

  @SuppressWarnings({"UnusedDeclaration"})
  @Deprecated
  public void setIntoLeads(boolean intoLeads) {
    this.intoLeads = intoLeads;
  }
}
