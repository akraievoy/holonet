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

package org.akraievoy.db.tx;

import com.google.common.base.Throwables;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;

class TransactionInvocationHandler implements InvocationHandler {
  protected final TransactionContextBase ctx;
  protected final TransactionAware impl;

  public TransactionInvocationHandler(TransactionContextBase ctx, final TransactionAware impl) {
    this.ctx = ctx;
    this.impl = impl;
  }

  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (ctx.getDb().getDbConfig().isSync()) {
      synchronized (ctx.getDb()) {
        return invokeInternal(method, args);
      }
    } else {
      return invokeInternal(method, args);
    }
  }

  protected Object invokeInternal(Method method, Object[] args) throws Throwable {
    Connection connection = null;
    Statement commitStatement = null;
    boolean rollback = true;

    try {
      connection = ctx.getDb().getDataSource().getConnection();

      ctx.setConn(connection);
      impl.setCtx(ctx);

      final Object result = invoke(method, args);

      commitStatement = connection.createStatement();
      commitStatement.execute("commit");

      rollback = false;

      return result;
    } finally {
      ctx.setConn(null);
      impl.setCtx(null);

      ctx.getDb().silentClose(commitStatement);
      ctx.getDb().silentClose(connection, rollback);
    }
  }

  protected Object invoke(Method method, Object[] args) throws Throwable {
    try {
      return method.invoke(impl, args);
    } catch (IllegalAccessException e) {
      throw Throwables.propagate(e);
    } catch (IllegalArgumentException e) {
      throw Throwables.propagate(e);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }
}
