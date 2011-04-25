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

package org.akraievoy.base.runner.vo;

import com.google.common.base.Objects;
import org.akraievoy.base.Die;

public class ParameterRanged implements Parameter {
  public static final String SEPARATOR_REGEX = "-";

  private final String name;
  private final long valueStart;
  private final long valueEnd;
  private String desc;
  private boolean internal;

  public ParameterRanged(final String name, final String valueSpec) {
    this(name, valueSpec.split(SEPARATOR_REGEX));
  }

  protected ParameterRanged(final String newName, final String[] newValues) {
    Die.ifFalse("newValues.length == 2", newValues.length == 2);

    name = newName;

    valueStart = Long.parseLong(newValues[0]);
    valueEnd = Long.parseLong(newValues[1]);
  }

  public String getName() {
    return name;
  }

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public boolean isInternal() {
    return internal;
  }

  public void setInternal(boolean internal) {
    this.internal = internal;
  }

  public long getValueCount() {
    return Math.abs(valueEnd - valueStart) + 1;
  }

  public String getValue(final long index) {
    final long vCount = getValueCount();

    Die.ifFalse("index < vCount", index < vCount);

    if (valueEnd >= valueStart) {
      return String.valueOf(valueStart + index);
    } else {
      return String.valueOf(valueStart - index);
    }
  }

  public boolean hasSameValues(Parameter param) {
    if (param instanceof ParameterRanged) {
      final ParameterRanged ranged = (ParameterRanged) param;
      return valueStart == ranged.valueStart && valueEnd == ranged.valueEnd;
    }

    if (getValueCount() != param.getValueCount()) {
      return false;
    }

    for (int i = 0; i < getValueCount(); i++) {
      if (!Objects.equal(getValue(i), param.getValue(i))) {
        return false;
      }
    }

    return true;
  }

  public void validatePos(long pos) {
    Die.ifTrue("pos < 0", pos < 0);
    Die.ifTrue("pos >= valueCount", pos >= getValueCount());
  }

  public String[] getValues() {
    final String[] values = new String[(int) getValueCount()];

    for (int i = 0; i < values.length; i++) {
      values[i] = getValue(i);
    }

    return values;
  }

  public String toString() {
    return String.valueOf(name) + "[" + getValueCount() + "] {" + valueStart + ".." + valueEnd + "}";
  }
}