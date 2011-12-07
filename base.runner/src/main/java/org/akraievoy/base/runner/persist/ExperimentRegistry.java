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

import org.akraievoy.base.runner.vo.*;

import java.sql.SQLException;
import java.util.List;

public interface ExperimentRegistry {
  List<String> listExperimentPaths() throws SQLException;

  Experiment findExperimentByPath(final String key) throws SQLException;

  boolean insertExperiment(final String expId, String path, final String depends, String name, String springXml, long fileModDate) throws SQLException;

  List<IdName> listConfs(long expUid) throws SQLException;

  Conf findConfByUid(long expUid, long confUid) throws SQLException;

  Conf findConfByPath(long expUid, String confPath) throws SQLException;

  long insertConf(long expUid, String confName,
                  String confDesc) throws SQLException;

  //  TODO remove this method in favor of findConfByUid
  Conf findConfById(long id) throws SQLException;

  List<Parameter> listParametersForConf(long confUid) throws SQLException;

  int insertParam(
      long confUid, String name, String value, Parameter.Strategy strategy, Parameter.Strategy chainedStrategy, String desc
  ) throws SQLException;
}
