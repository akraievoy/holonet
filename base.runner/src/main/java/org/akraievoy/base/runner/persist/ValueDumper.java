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

package org.akraievoy.base.runner.persist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;

public class ValueDumper {
  private static final Logger log = LoggerFactory.getLogger(ValueDumper.class);

  protected String getType(Object attrValue) {
    return attrValue.getClass().getName();
  }

  protected boolean isObjPrimitive(Object attrValue) {
    final String type = getType(attrValue);

    return type.startsWith("java.lang.");
  }

  protected boolean isCNamePrimitive(String attrType) {
    return attrType.startsWith("java.lang.");
  }

  protected Object rsToPrimitive(String attrType, String stringVal) throws SQLException {
    final Class attrClass;
    try {
      attrClass = Class.forName(attrType);
    } catch (ClassNotFoundException e) {
      throw new SQLException(e);
    }

    if (attrType.equals("java.lang.String")) {
      return stringVal;
    }

    try {
      final Method valueOfMethod = attrClass.getMethod("valueOf", String.class);
      return valueOfMethod.invoke(null, stringVal);
    } catch (NoSuchMethodException e) {
      throw new SQLException(e);
    } catch (InvocationTargetException e) {
      throw new SQLException(e);
    } catch (IllegalAccessException e) {
      throw new SQLException(e);
    }
  }

  protected String objToPrimitive(Object attrValue) {
    return String.valueOf(attrValue);
  }

  protected Class rsToClass(String attrType) throws SQLException {
    final Class attrClass;
    try {
      attrClass = Class.forName(attrType);
    } catch (ClassNotFoundException e) {
      throw new SQLException(e);
    }
    return attrClass;
  }
}
