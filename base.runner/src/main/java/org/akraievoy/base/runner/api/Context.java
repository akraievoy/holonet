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

package org.akraievoy.base.runner.api;

import com.google.common.base.Throwables;
import org.akraievoy.base.runner.domain.ParamSetEnumerator;
import org.akraievoy.base.runner.persist.RunRegistry;
import org.akraievoy.base.runner.vo.Parameter;
import org.akraievoy.base.runner.vo.RunInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;

public class Context {
  private static final Logger log = LoggerFactory.getLogger(Context.class);

  protected final ParamSetEnumerator widenedPse;
  protected final RunRegistry dao;
  protected final long runId;

  protected final SortedMap<Long, RunInfo> runChain;

  public Context(ParamSetEnumerator widenedPse, RunRegistry dao, long runId, SortedMap<Long, RunInfo> runChain) {
    this.widenedPse = widenedPse;
    this.dao = dao;
    this.runId = runId;
    this.runChain = runChain;
  }

  public <E> E get(String path, Class<E> attrType, final boolean cache) {
    if (cache) {
      final E cached = getInternal(path, attrType, widenedPse);
      if (cached != null) {
        return (E) cached;
      }
    }

    return (E) getInternal(path, attrType, widenedPse);
  }

  public <E> E get(String path, Class<E> attrType, Map<String, Integer> offset) {
    return (E) getInternal(path, attrType, widenedPse.dupe(offset));
  }

  public void put(String path, Object attrvalue, Map<String, Integer> offset) {
    putInternal(path, attrvalue, widenedPse.dupe(offset).getIndex(false));
  }

  public void put(String path, Object attrValue) {
    putInternal(path, attrValue, widenedPse.getIndex(false));
  }

  public boolean containsKey(String path) {
    try {
      final boolean value = dao.findCtxAttrNoLoad(runId, widenedPse.getIndex(false), path);

      if (value) {
        return true;
      }

      for (RunInfo chained : runChain.values()) {
        final long translated = widenedPse.translateIndex(chained.getEnumerator(), false);

        final boolean chainedValue = dao.findCtxAttrNoLoad(
            chained.getRun().getUid(),
            translated,
            path
        );

        if (chainedValue) {
          return chainedValue;
        }
      }
    } catch (SQLException e) {
      log.warn("failed to load value from database: {}", Throwables.getRootCause(e).toString());
      log.debug("[detailed trace]", e);
    }

    return false;
  }

  @SuppressWarnings({"unchecked"})
  protected <E> E getInternal(String path, Class<E> attrType, ParamSetEnumerator widenedPse) {
    try {
      final Object value = dao.findCtxAttr(runId, widenedPse.getIndex(false), path);

      if (value != null) {
        return (E) value;
      }

      for (RunInfo chained : runChain.values()) {
        final long translated = widenedPse.translateIndex(chained.getEnumerator(), false);

        final Object chainedValue = dao.findCtxAttr(
            chained.getRun().getUid(),
            translated,
            path
        );

        if (chainedValue != null) {
          return (E) chainedValue;
        }
      }
    } catch (SQLException e) {
      log.warn("failed to load value from database: {}", Throwables.getRootCause(e).toString());
      log.debug("[detailed trace]", e);
    }

    return null;
  }

  protected void putInternal(String path, Object attrValue, long psetIndex) {
    log.debug(
        "{}[{}] <- {}: {}",
        new Object[]{path, psetIndex, attrValue.getClass().getSimpleName(), attrValue}
    );

    try {
      dao.insertCtxAttr(runId, psetIndex, path, attrValue);
    } catch (SQLException e) {
      log.warn("failed to persist value to database: {}", Throwables.getRootCause(e).toString());
      log.debug("[detailed trace]", e);
    }
  }

  public String[] listPaths() {
    List<String> paths = new ArrayList<String>();

    try {
      paths.addAll(dao.listCtxPaths(runId));
      for (RunInfo chain : runChain.values()) {
        paths.addAll(dao.listCtxPaths(chain.getRun().getUid()));
      }

      return paths.toArray(new String[paths.size()]);
    } catch (SQLException e) {
      log.warn("failed to list values: {}", Throwables.getRootCause(e).toString());
      log.debug("[detailed trace]", e);
      return new String[0];
    }
  }

  protected String keyPrefix(final long psetIndex) {
    return String.valueOf(psetIndex) + ":";
  }

  public ParamSetEnumerator getEnumerator() {
    return widenedPse;
  }

  public long getCount(String paramName) {
    final int parameterIndex = widenedPse.getParameterIndex(paramName);
    final Parameter parameter = widenedPse.getParameter(parameterIndex);
    final long count = parameter.getValueCount();

    return count;
  }

  public static Map<String, Integer> offset() { return new TreeMap<String, Integer>(); }

  public static Map<String, Integer> offset(
      String paramName, int offset
  ) {
    final Map<String, Integer> offsetMap = new TreeMap<String, Integer>();

    offsetMap.put(paramName, offset);

    return offsetMap;
  }

  public static Map<String, Integer> offset(
      String param1, int offset1,
      String param2, int offset2
  ) {
    final Map<String, Integer> offsetMap = new TreeMap<String, Integer>();

    offsetMap.put(param1, offset1);
    offsetMap.put(param2, offset2);

    return offsetMap;
  }
}
