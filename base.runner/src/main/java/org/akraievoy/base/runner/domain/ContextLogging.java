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

import com.google.common.base.Throwables;
import org.akraievoy.base.runner.api.ParamSetEnumerator;
import org.akraievoy.base.runner.persist.RunRegistry;
import org.akraievoy.base.runner.vo.RunInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class ContextLogging extends ContextBase {
  private static final Logger log = LoggerFactory.getLogger(ContextLogging.class);

  public ContextLogging(long runId, RunRegistry dao, ParamSetEnumerator enumerator, RunInfo[] runChain) {
    super(enumerator, dao, runId, runChain);
  }

  public void put(String path, Object attrValue, final boolean cache) {
    log.debug("{} <- {}: {}", new Object[]{path, attrValue.getClass().getSimpleName(), attrValue});

    super.put(path, attrValue, cache);
  }

  protected void onPutDbFailure(SQLException e) {
    log.warn("failed to persist value to database: {}", Throwables.getRootCause(e).toString());
    log.debug("[detailed trace]", e);
  }

  protected void onGetDbFailure(SQLException e) {
    log.warn("failed to load value from database: {}", Throwables.getRootCause(e).toString());
    log.debug("[detailed trace]", e);
  }

  protected void onListDbFailure(SQLException e) {
    log.warn("failed to list values: {}", Throwables.getRootCause(e).toString());
    log.debug("[detailed trace]", e);
  }
}
