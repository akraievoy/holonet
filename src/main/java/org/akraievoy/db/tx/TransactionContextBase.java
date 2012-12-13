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

import org.akraievoy.db.Db;
import org.apache.commons.dbutils.QueryRunner;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

public class TransactionContextBase implements TransactionContext {
  protected final ThreadLocal<Connection> connTL = new ThreadLocal<Connection>();
  protected final Db db;

  public TransactionContextBase(final Db db) {
    this.db = db;
  }

  public Connection getConn() {
    return connTL.get();
  }

  public void setConn(Connection conn) {
    connTL.set(conn);
  }

  public QueryRunner getQueryRunner() {
    return db.getQueryRunner();
  }

  public Db getDb() {
    return db;
  }

  public void runScript(InputStream scriptStream) throws SQLException {
    getDb().runScript(getConn(), scriptStream);
  }

  @SuppressWarnings({"unchecked"})
  public <E extends TransactionAware> E compose(Class<E> interf, final E impl) {
    return (E) Proxy.newProxyInstance(
        interf.getClassLoader(),
        new Class[]{interf},
        new TransactionInvocationHandler(this, impl));
  }
}
