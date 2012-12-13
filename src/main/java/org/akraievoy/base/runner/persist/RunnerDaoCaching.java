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

import org.akraievoy.base.runner.vo.Experiment;
import org.akraievoy.base.runner.vo.Run;
import org.akraievoy.db.QueryRegistry;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunnerDaoCaching extends RunnerDaoBase {
  protected final List<String> experimentPathCache = new ArrayList<String>();
  protected final Map<String, Experiment> experimentCache = new HashMap<String, Experiment>();
  protected Run[] runCache = null;

  public RunnerDaoCaching(QueryRegistry q) {
    super(q, new ValueDumper());
  }

  public List<String> listExperimentPaths() throws SQLException {
    if (!experimentPathCache.isEmpty()) {
      return new ArrayList<String>(experimentPathCache);
    }

    final List<String> result = super.listExperimentPaths();

    experimentPathCache.addAll(result);

    return result;
  }

  public Experiment findExperimentByPath(String path) throws SQLException {
    final Experiment cachedExperiment = experimentCache.get(path);
    if (cachedExperiment != null) {
      return cachedExperiment;
    }

    final Experiment experiment = super.findExperimentByPath(path);

    experimentCache.put(path, experiment);

    return experiment;
  }

  protected int insertExperimentNoCheck(final String expId, String path, final String depends, String desc, String springXml) throws SQLException {
    final int updateCount = super.insertExperimentNoCheck(expId, path, depends, desc, springXml);

    experimentCache.remove(path);
    experimentPathCache.clear();

    return updateCount;
  }

  public Run[] listRuns() throws SQLException {
    if (runCache == null) {
      runCache = super.listRuns();
    }

    return runCache;
  }

  public long insertRun(long confUid, long[] chain, final long psetCount) throws SQLException {
    runCache = null;

    return super.insertRun(confUid, chain, psetCount);
  }

  public boolean updateRunPsetComplete(long runUid, long psetComplete) throws SQLException {
    runCache = null;

    return super.updateRunPsetComplete(runUid, psetComplete);
  }
}
