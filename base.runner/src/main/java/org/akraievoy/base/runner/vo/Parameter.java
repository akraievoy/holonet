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

import com.google.common.base.Strings;
import org.akraievoy.base.Die;
import org.akraievoy.base.ObjArrays;

import javax.annotation.Nullable;
import java.util.Arrays;

public class Parameter {
  public static enum Strategy {
    /**
     * Use in current and propagate parameter to chained experiments.
     */
    ITERATE,
    /**
     * Use only the first value of parameter in current and chained experiments.
     */
    USE_FIRST,
    /**
     * Use only the last value of parameter in current and chained experiments.
     */
    USE_LAST;

    public static Strategy fromString(String str) {
      return Strings.isNullOrEmpty(str) ? ITERATE : valueOf(str.toUpperCase());
    }
  }

  private static final String TOKEN_LIST = ";";
  private static final String TOKEN_RANGE = "-";

  private static final String PARAM_VALUE_STRATEGY = "*strategy*";

  private final String name;
  private final String[] values;
  private String desc;
  private Strategy strategyCurrent = Strategy.ITERATE;
  private Strategy strategyChained = Strategy.ITERATE;
  private long runUid = 0;
  private boolean chained = false;

  protected Parameter(final String newName, final String[] newValues) {
    name = newName;
    values = newValues.clone();
  }

  public String getName() { return name; }

  public Strategy getStrategyCurrent() { return strategyCurrent; }
  public void setStrategyCurrent(Strategy strategyCurrent) { this.strategyCurrent = strategyCurrent; }

  public Strategy getStrategyChained() { return strategyChained; }
  public void setStrategyChained(Strategy strategyChained) { this.strategyChained = strategyChained; }

  public String getDesc() { return desc; }
  public void setDesc(String desc) { this.desc = desc; }

  public String[] getValues() { return values.clone(); }
  public long getValueCount() { return values.length; }

  public boolean isChained() { return chained; }
  public void setChained(boolean chained) { this.chained = chained; }

  public long getRunUid() { return runUid; }
  public void setRunUid(long runUid) { this.runUid = runUid; }

  public Parameter copy() {
    final Parameter copy = new Parameter(name, values);

    copy.strategyCurrent = strategyCurrent;
    copy.strategyChained = strategyChained;
    copy.desc = desc;
    copy.chained = chained;
    copy.runUid = runUid;

    return copy;
  }
  public String getValue(final long index) {
    validatePos(index);
    return values[(int) index];
  }

  public boolean sameValues(Parameter param) {
    return this == param || param != null && Arrays.equals(getValues(), param.getValues());
  }

  public void validatePos(long pos) {
    Die.ifTrue("pos < 0", pos < 0);
    Die.ifTrue("pos >= valueCount", pos >= getValueCount());
  }

  public String toString() {
    if (values == null) {
      return String.valueOf(name);
    }

    return String.valueOf(name) + "[" + getValueCount() + "] {" + Arrays.deepToString(values) + "}";
  }

  public static Parameter create(final String name, final String valueSpec) {
    final String[] valuesArr;

    if (valueSpec.indexOf(TOKEN_RANGE) <= 0) {
      valuesArr = valueSpec.split(TOKEN_LIST);
    } else {
      final String[] valueSpecArr = valueSpec.split(TOKEN_RANGE);
      Die.ifFalse("values.length == 2", valueSpecArr.length == 2);

      long valueStart = Long.parseLong(valueSpecArr[0]);
      long valueEnd = Long.parseLong(valueSpecArr[1]);

      final long valueCount = Math.abs(valueEnd - valueStart) + 1;
      Die.ifTrue("valueCount > Integer.MAX_VALUE", valueCount > Integer.MAX_VALUE);
      valuesArr = new String[(int) valueCount];

      for (int i = 0; i < valueCount; i++) {
        valuesArr[i] = String.valueOf(valueStart + (valueEnd >= valueStart ? i : -i));
      }
    }

    return new Parameter(name, valuesArr);
  }

  public boolean isStrategy() {
    return ObjArrays.contains(getValues(), PARAM_VALUE_STRATEGY);
  }

  public void applyStrategy(@Nullable Parameter strategy) {
    if (strategy == null) {
      return;
    }

    setStrategyCurrent(strategy.getStrategyCurrent());
    setStrategyChained(strategy.getStrategyChained());
  }

  public Strategy getStrategy() {
    return chained ? strategyChained : strategyCurrent;
  }

  public long getInitialPos() {
    return getStrategy() == Strategy.USE_LAST ? getValueCount() - 1 : 0L;
  }
}