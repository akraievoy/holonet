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

package org.akraievoy.base.runner.domain;

import org.akraievoy.base.Die;
import org.akraievoy.base.runner.vo.Parameter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class ParamSetEnumerator {
  public static final int COUNT_EMPTY = 1;

  protected final List<Parameter> params = new ArrayList<Parameter>();
  protected final Map<String, Parameter> strategies = new TreeMap<String, Parameter>();
  protected final List<Long> paramPoses = new ArrayList<Long>();

  public boolean increment(final boolean iterated, final boolean spawned) {
    int incrementPos = paramPoses.size() - 1;
    boolean overflow;
    while (
        incrementPos >= 0 && (
            fixed(incrementPos, iterated, spawned) |
            (overflow = overflow(incrementPos))
        )
    ) {
      if (overflow) {
        paramPoses.set(incrementPos, 0L);
      }
      incrementPos--;
    }

    if (incrementPos < 0) {
      return false;
    }

    //  we already zeroed all iterable and overflowed axes after this position 
    paramPoses.set(incrementPos, paramPoses.get(incrementPos) + 1);
    return true;
  }

  private boolean fixed(int idx, boolean iterated, boolean spawned) {
    final Parameter.Strategy strategy = params.get(idx).getStrategy();

    return Parameter.Strategy.fixed(strategy) ||
        iterated && Parameter.Strategy.ITERATE == strategy ||
        spawned && Parameter.Strategy.SPAWN == strategy;

  }

  protected boolean overflow(int idx) {
    final Parameter param = params.get(idx);
    if (!Parameter.Strategy.full(param.getStrategy()))  {
      return false;
    }

    final long pos = paramPoses.get(idx);
    final boolean overflow = pos == param.getValueCount() - 1;

    return overflow;
  }

  public void load(final List<Parameter> myParams, long runUid) {
    Die.ifNull("params", params);

    params.clear();
    strategies.clear();

    for (Parameter param : myParams) {
      final Parameter copy = param.copy();
      copy.setRunUid(runUid);

      if (param.isStrategy()) {
        processStrategy(param);
      } else {
        params.add(copy);
      }
    }

    restart();
  }

  public Properties getProperties() {
    Properties props = new Properties();

    for (int i = 0, paramsSize = params.size(); i < paramsSize; i++) {
      final Parameter param = params.get(i);
      final long paramPos = paramPoses.get(i);

      props.setProperty(param.getName(), param.getValue(paramPos));
    }

    return props;
  }

  public String[][] getParamValues(String[][] paramValues) {
    final boolean noProvided = paramValues == null || paramValues.length != params.size();
    final String[][] result = noProvided ? new String[params.size()][] : paramValues;

    for (int i = 0, paramsSize = params.size(); i < paramsSize; i++) {
      final Parameter param = params.get(i);
      final long paramPos = paramPoses.get(i);

      if (result[i] == null || result[i].length < 2) {
        result[i] = new String[]{param.getName(), param.getValue(paramPos)};
      } else {
        result[i][0] = param.getName();
        result[i][1] = param.getValue(paramPos);
      }
    }

    return result;
  }

  public ParamSetEnumerator dupe(final Map<String, Integer> offset) {
    final ParamSetEnumerator dupe = new ParamSetEnumerator();

    dupe.strategies.putAll(strategies);
    dupe.params.addAll(params);
    dupe.paramPoses.addAll(paramPoses);

    if (offset != null) {
      for (String paramName : offset.keySet()) {
        final int paramIndex = getParameterIndex(paramName);
        final Parameter param = params.get(paramIndex);
        Die.ifFalse("paramIndex >= 0", paramIndex >= 0);

        dupe.paramPoses.set(paramIndex, dupe.paramPoses.get(paramIndex) + offset.get(paramName));
        param.validatePos(dupe.paramPoses.get(paramIndex));
      }
    }

    return dupe;
  }

  public void widen(final ParamSetEnumerator widenerPse) {
    for (String name : widenerPse.strategies.keySet()) {
      processStrategy(widenerPse.strategies.get(name).copy());
    }

    for (Parameter widenerParam : widenerPse.params) {
      final int rootIndex = getParameterIndex(widenerParam.getName());
      if (rootIndex >= 0) {
        continue;
      }

      final Parameter copy = widenerParam.copy();
      copy.setChained(true);

      final Parameter strategy = strategies.get(copy.getName());
      copy.applyStrategy(strategy);

      params.add(copy);
      paramPoses.add(copy.getInitialPos());
    }
  }

  protected void processStrategy(Parameter strategy) {
    Die.ifFalse("strategy.isStrategy()", strategy.isStrategy());

    final String sName = strategy.getName();
    if (strategies.get(sName) == null || strategies.get(sName).getRunUid() < strategy.getRunUid()) {
      strategies.put(sName, strategy);

      final Parameter paramForStrategy = getParameter(sName);
      if (paramForStrategy != null) {
        paramForStrategy.applyStrategy(strategy);
        paramPoses.set(getParameterIndex(sName), paramForStrategy.getInitialPos());
      }
    }
  }

  public void narrow(ParamSetEnumerator widenedPse) {
    for (int i = 0, paramsSize = params.size(); i < paramsSize; i++) {
      final int widenedI = widenedPse.getParameterIndex(params.get(i).getName());
      Die.ifTrue("widenedI < 0", widenedI < 0);

      paramPoses.set(i, widenedPse.paramPoses.get(widenedI));
    }
  }

  public long getCount() {
    return getCount(null);
  }

  public long getCount(List<String> subsetNames) {
    if (params.isEmpty() || subsetNames != null && subsetNames.isEmpty()) {
      return COUNT_EMPTY;
    }

    long result = COUNT_EMPTY;
    long fixed = COUNT_EMPTY;
    for (Parameter param : params) {
      if (subsetNames != null && !subsetNames.contains(param.getName())) {
        continue;
      }

      if (Parameter.Strategy.full(param.getStrategy())) {
        result *= param.getValueCount();
      } else {
        fixed *= param.getValueCount();
      }
    }

    return result * fixed;
  }

  public long getIndex(final boolean globFixed) {
    return getIndexForPos(paramPoses, globFixed);
  }

  public boolean isFirst() {
    return getIndex(false) == COUNT_EMPTY;
  }

  protected long getIndexForPos(final List<Long> paramPoses, final boolean globFixed) {
    if (params.size() == 0) {
      return COUNT_EMPTY;
    }

    long result = COUNT_EMPTY;
    long fixed = COUNT_EMPTY;
    long pow = 1;
    for (int paramI = params.size() - 1; paramI >= 0; paramI--) {
      final Parameter param = params.get(paramI);
      if (Parameter.Strategy.full(param.getStrategy()) || !globFixed) {
        result += pow * paramPoses.get(paramI);
        pow *= param.getValueCount();
      } else {
        fixed *= param.getValueCount();
      }
    }

    return result * fixed;
  }

  public Parameter getParameter(int index) {
    return index < 0 ? params.get(params.size() + index) : params.get(index);
  }

  public @Nullable Parameter getParameter(@Nonnull String name) {
    for (final Parameter param : params) {
      if (param.getName().equals(name)) {
        return param;
      }
    }

    return null;
  }

  public int getParameterIndex(String name) {
    for (int index = 0; index < params.size(); index++) {
      final Parameter param = params.get(index);
      if (param.getName().equals(name)) {
        return index;
      }
    }

    return -1;
  }

  public ParamSetEnumerator restart() {
    paramPoses.clear();
    for (Parameter param : params) {
      paramPoses.add(param.getInitialPos());
    }
    return this;
  }

  public Parameter findCollision(ParamSetEnumerator chainedEnumerator) {
    for (Parameter chainedParam : chainedEnumerator.params) {
      final int rootIndex = getParameterIndex(chainedParam.getName());
      if (rootIndex < 0) {
        continue;
      }

      final Parameter rootParam = getParameter(rootIndex);
      if (!rootParam.sameValues(chainedParam)) {
        return rootParam;
      }
    }

    return null;
  }

  //  LATER simplify
  public long translateIndex(ParamSetEnumerator chainedEnumerator, final boolean globFixed) {
    if (params.size() == 0) {
      return 0;
    }

    final List<Long> chainedPoses = new ArrayList<Long>(chainedEnumerator.paramPoses);
    for (int i = 0, paramsSize = params.size(); i < paramsSize; i++) {
      Parameter rootParam = params.get(i);
      final int chainedIndex = chainedEnumerator.getParameterIndex(rootParam.getName());
      if (chainedIndex < 0) {
        continue;
      }

      chainedPoses.set(chainedIndex, paramPoses.get(i));
    }

    return chainedEnumerator.getIndexForPos(chainedPoses, globFixed);
  }

  public int getParameterCount() {
    return params.size();
  }

  public long getPos(String paramName) {
    return paramPoses.get(getParameterIndex(paramName));  
  }

  public Map<String, Integer> asOffsets(ParamSetEnumerator baseParams) {
    final Map<String, Integer> offsets = new TreeMap<String, Integer>();

    for (int paramIndex = 0; paramIndex < params.size(); paramIndex++) {
      final String paramName = params.get(paramIndex).getName();
      offsets.put(paramName, (int) (paramPoses.get(paramIndex) - baseParams.getPos(paramName)));
    }

    return offsets;
  }
}
