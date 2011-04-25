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

import com.google.common.base.Throwables;
import org.akraievoy.base.Die;
import org.akraievoy.base.Startable;
import org.apache.commons.dbutils.QueryLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class QueryRegistry implements Startable {
  private static final Logger log = LoggerFactory.getLogger(QueryRegistry.class);

  protected Map queries;
  protected String queryResourcePath;

  public QueryRegistry(final String queryResourcePath) {
    this.queryResourcePath = queryResourcePath;
  }

  public void start() {
    queries = loadQueries();
  }

  public void stop() {
    queries.clear();
  }

  protected Map loadQueries() {
    try {
      final QueryLoader loader = QueryLoader.instance();
      return loader.load(queryResourcePath);
    } catch (IOException e) {
      log.warn("loadQueries() " + Throwables.getRootCause(e).toString());
      throw Die.unexpected("queryResourcePath", queryResourcePath, "queries file not found or corrupt");
    }
  }

  public String getQuery(final String key) {
    final String query = (String) queries.get(key);
    Die.ifEmpty("queries['" + key + "']", query);

    return query;
  }
}