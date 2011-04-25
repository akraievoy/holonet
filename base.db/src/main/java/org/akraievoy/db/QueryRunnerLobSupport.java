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

import org.apache.commons.dbutils.QueryRunner;

import javax.sql.DataSource;
import java.io.InputStream;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class QueryRunnerLobSupport extends QueryRunner {
  public QueryRunnerLobSupport(DataSource dataSource) {
    super(dataSource);
  }

  protected void fillStatement(PreparedStatement stmt, Object[] params) throws SQLException {
    if (params == null) {
      return;
    }

    for (int i = 0; i < params.length; i++) {
      if (params[i] != null) {
        if (params[i] instanceof Reader) {
          stmt.setClob(i + 1, (Reader) params[i]);
        } else if (params[i] instanceof InputStream) {
          stmt.setBlob(i + 1, (InputStream) params[i]);
        } else {
          stmt.setObject(i + 1, params[i]);
        }
      } else {
        // VARCHAR works with many drivers regardless
        // of the actual column type.  Oddly, NULL and
        // OTHER don't work with Oracle's drivers.
        stmt.setNull(i + 1, Types.VARCHAR);
      }
    }
  }
}
