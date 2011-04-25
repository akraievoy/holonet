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

import org.akraievoy.base.runner.vo.Run;
import org.akraievoy.base.runner.vo.RunInfo;

import java.sql.SQLException;
import java.util.List;

public interface RunRegistry {
  long insertRun(long confUid, long[] chain, final long psetCount) throws SQLException;

  Run findRun(long runUid) throws SQLException;

  boolean insertCtxAttr(long runUid, long index, String path, Object attrValue) throws SQLException;

  Object findCtxAttr(long runUid, long index, String path) throws SQLException;

  List<String> listCtxPaths(long runUid) throws SQLException;

  Run[] listRuns() throws SQLException;

  RunInfo[] loadChainedRuns(List<Long> chainedRunIds);

  boolean updateRunPsetComplete(long runId, final long psetComplete) throws SQLException;

  boolean findCtxAttrNoLoad(long runId, long index, String path) throws SQLException;
}
