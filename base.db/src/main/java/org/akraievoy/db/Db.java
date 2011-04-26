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

package org.akraievoy.db;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.io.Closeables;
import org.akraievoy.base.Die;
import org.akraievoy.base.Startable;
import org.apache.commons.dbutils.QueryRunner;
import org.h2.jdbcx.JdbcConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Db implements Startable {
  private static final Logger log = LoggerFactory.getLogger(Db.class);

  protected final DbConfig dbConfig;

  protected DataSource poolingDataSource;
  protected QueryRunner queryRunner;

  public Db(final DbConfig dbConfig) {
    this.dbConfig = dbConfig;
  }

  public DbConfig getDbConfig() {
    return dbConfig;
  }

  public synchronized DataSource getDataSource() {
    if (poolingDataSource == null) {
      poolingDataSource = lookupDataSource();
    }

    return poolingDataSource;
  }

  protected DataSource lookupDataSource() {
    log.debug("url: '{}'", dbConfig.getUrl());

    try {
      Class.forName(dbConfig.getDriverClass());
    } catch (ClassNotFoundException e) {
      throw Throwables.propagate(e);
    }

    final JdbcConnectionPool dataSource = JdbcConnectionPool.create(
        dbConfig.getUrl(),
        dbConfig.getLogin(),
        dbConfig.getPassword()
    );

    dataSource.setMaxConnections(dbConfig.getConnectionMaxNum());
    dataSource.setMaxConnections(dbConfig.getConnectionTimeout());

    return dataSource;
  }

  public synchronized QueryRunner getQueryRunner() {
    if (queryRunner == null) {
      queryRunner = new QueryRunnerLobSupport(getDataSource());
    }

    return queryRunner;
  }

  public void start() {
    final DataSource dataSource = lookupDataSource();
    Die.ifNull("dataSource", dataSource);
  }

  public void stop() {
    poolingDataSource = null;
    queryRunner = null;
  }

  public int runScript(final Connection connection, final InputStream scriptIn) throws SQLException {
    Die.ifNull("connection", connection);
    Die.ifNull("scriptIn", scriptIn);

    BufferedReader reader = null;
    int statementsRun = 0;
    String nextSql = "";

    try {
      reader = new BufferedReader(new InputStreamReader(scriptIn));

      ScriptReader scriptReader = new ScriptReader(reader);
      Statement statement = connection.createStatement();

      while (!Strings.isNullOrEmpty(nextSql = scriptReader.readStatement())) {
        statement.execute(nextSql);
        statementsRun++;
      }

      return statementsRun;
    } catch (SQLException e) {
      log.error("failed on sql #" + statementsRun + ": '" + nextSql + "'");
      throw e;
    } finally {
      Closeables.closeQuietly(reader);
    }
  }

  public void silentClose(Connection connection, final boolean rollback) {
    try {
      if (connection != null) {
        if (rollback) {
          connection.rollback();
        }

        connection.close();
      }
    } catch (SQLException e) {
      log.warn("failed to close connection: " + e.getMessage());
    }
  }

  public void silentClose(Statement statement) {
    try {
      if (statement != null)
        statement.close();
    } catch (SQLException e) {
      log.warn("failed to close statement: " + e.getMessage());
    }
  }
}
