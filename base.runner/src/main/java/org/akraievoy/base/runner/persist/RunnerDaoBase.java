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

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import com.google.common.io.InputSupplier;
import org.akraievoy.base.Die;
import org.akraievoy.base.Format;
import org.akraievoy.base.Parse;
import org.akraievoy.base.runner.domain.ParamSetEnumerator;
import org.akraievoy.base.runner.vo.*;
import org.akraievoy.db.QueryRegistry;
import org.akraievoy.db.tx.TransactionContext;
import org.akraievoy.db.tx.TransactionContextBase;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class RunnerDaoBase implements RunnerDao {
  private static final Logger log = LoggerFactory.getLogger(RunnerDaoBase.class);

  protected final ValueDumper valueDumper;

  protected final QueryRegistry q;
  protected TransactionContext ctx;
  protected int schemaVersion = 1;

  public RunnerDaoBase(QueryRegistry q, ValueDumper valueDumper) {
    this.q = q;
    this.valueDumper = valueDumper;
  }

  public void setSchemaVersion(int schemaVersion) {
    this.schemaVersion = schemaVersion;
  }

  public TransactionContext getCtx() {
    return ctx;
  }

  public void setCtx(TransactionContext tCtx) {
    this.ctx = tCtx;
  }

  public void initSchema() throws SQLException {
    int schemaVersion;
    try {
      schemaVersion = getSchemaVersion();
    } catch (SQLException e) {
      log.warn("Schema corrupt: {}", Throwables.getRootCause(e).toString());
      schemaVersion = 0;
    }

    while (schemaVersion < this.schemaVersion) {
      final String path = "/org/akraievoy/base/runner/persist/schema." + schemaVersion + ".sql";
      final InputStream scriptStream = this.getClass().getResourceAsStream(path);
      getCtx().runScript(scriptStream);
      schemaVersion = getSchemaVersion();
    }
  }

  public int getSchemaVersion() throws SQLException {
    final String schemaVersionStr = (String) getCtx().getQueryRunner().query(
        getCtx().getConn(),
        q.getQuery("info.findSchemaVersion"),
        new ScalarHandler()
    );

    final int schemaVersion = Parse.oneInt(schemaVersionStr, 0);

    return schemaVersion;
  }

  public long insertRun(long confUid, long[] chain, final long psetCount) throws SQLException {
    final int updateCount = getCtx().getQueryRunner().update(
        getCtx().getConn(),
        q.getQuery("run.insertNew"),
        new Object[]{
            confUid,
            System.currentTimeMillis(),
            Format.format(chain),
            psetCount
        }
    );

    Die.ifFalse("updateCount > 0", updateCount > 0);

    final Long lastId = (Long) getCtx().getQueryRunner().query(
        getCtx().getConn(),
        q.getQuery("run.findLastUid"),
        new ScalarHandler()
    );

    Die.ifNull("lastId", lastId);

    return lastId;
  }

  public Run findRun(final long runUid) throws SQLException {
    final Run run = (Run) getCtx().getQueryRunner().query(
        getCtx().getConn(),
        q.getQuery("run.findById"),
        new Object[]{runUid},
        new BeanHandler(Run.class, new RunRowProcessor(true))
    );

    return run;
  }

  @SuppressWarnings({"unchecked"})
  public Run[] listRuns() throws SQLException {
    final List runs = (List) getCtx().getQueryRunner().query(
        getCtx().getConn(),
        q.getQuery("run.findAll"),
        new BeanListHandler(Run.class, new RunRowProcessor(true))
    );

    return (Run[]) runs.toArray(new Run[runs.size()]);
  }

  public boolean insertCtxAttr(long runUid, long index, String path, Object attrValue) throws SQLException {
    if (attrValue == null) {
      return false;
    }

    final String type = valueDumper.getType(attrValue);
    if (valueDumper.isObjPrimitive(attrValue)) {
      final int updateCount = getCtx().getQueryRunner().update(
          getCtx().getConn(),
          q.getQuery("ctx.insertPrimitive"),
          new Object[]{System.currentTimeMillis(), runUid, index, path, type, valueDumper.objToPrimitive(attrValue)}
      );

      return updateCount > 0;
    }

    final InputStream dumpInput = valueDumper.createDumpInputStream(attrValue);

    final int updateCount = getCtx().getQueryRunner().update(
        getCtx().getConn(),
        q.getQuery("ctx.insertDumpable"),
        new Object[]{System.currentTimeMillis(), runUid, index, path, type, dumpInput}
    );

    return updateCount > 0;
  }

  public Object findCtxAttr(long runUid, long index, String path) throws SQLException {
    if (path == null) {
      return false;
    }

    final Object attrValue = getCtx().getQueryRunner().query(
        getCtx().getConn(),
        q.getQuery("ctx.findByRunSetPath"),
        new Object[]{runUid, index, path, runUid, index, path},
        new BeanHandler(Object.class, new CtxAttrRowProcessor(valueDumper))
    );

    return attrValue;
  }

  public List<String> listCtxPaths(final long runUid) throws SQLException {
    final List paths = (List) getCtx().getQueryRunner().query(
        getCtx().getConn(),
        q.getQuery("ctx.listPaths"),
        new Object[]{runUid},
        new ColumnListHandler()
    );

    final List<String> result = new ArrayList<String>(paths.size());

    //noinspection unchecked
    result.addAll(paths);

    return result;
  }

  public RunnerDao compose(final TransactionContextBase ctxBase) {
    final RunnerDao runnerDao = ctxBase.compose(RunnerDao.class, this);

    try {
      runnerDao.initSchema();
    } catch (SQLException e) {
      throw new IllegalStateException("failed to init schema", e);
    }

    return runnerDao;
  }

  public List<String> listExperimentPaths() throws SQLException {
    final List paths = (List) getCtx().getQueryRunner().query(
        getCtx().getConn(),
        q.getQuery("exp.listPaths"),
        new ColumnListHandler()
    );

    final List<String> result = new ArrayList<String>();

    //noinspection unchecked
    result.addAll(paths);

    return result;
  }

  public Experiment findExperimentByPath(String path) throws SQLException {
    final Experiment experiment = (Experiment) getCtx().getQueryRunner().query(
        getCtx().getConn(),
        q.getQuery("exp.findByPath"),
        new Object[]{path},
        new BeanHandler(ExperimentBean.class, new ExperimentRowProcessor())
    );

    return experiment;
  }

  public boolean insertExperiment(
      String expId, String path, String depends, String name, String springXml, long fileModDate
  ) throws SQLException {
    final Long maxStamp = (Long) getCtx().getQueryRunner().query(
        getCtx().getConn(),
        q.getQuery("exp.findLastStampForPath"),
        path,
        new ScalarHandler()
    );

    if (maxStamp != null && maxStamp >= fileModDate) {
      return false;
    }

    final int updateCount = insertExperimentNoCheck(expId, path, depends, name, springXml);

    return updateCount > 0;
  }

  protected int insertExperimentNoCheck(final String expId, String path, final String depends, String desc, String springXml) throws SQLException {
    final byte[] springXmlBytes = springXml.getBytes(Charset.forName("UTF-8"));
    final int updateCount = getCtx().getQueryRunner().update(
        getCtx().getConn(),
        q.getQuery("exp.insertNew"),
        new Object[]{
            expId,
            path,
            depends,
            desc,
            System.currentTimeMillis(),
            new ByteArrayInputStream(springXmlBytes)
        }
    );

    return updateCount;
  }

  public List<IdName> listConfs(final long expUid) throws SQLException {
    final List idNames = (List) getCtx().getQueryRunner().query(
        getCtx().getConn(),
        q.getQuery("conf.listPathsForExp"),
        new Object[]{expUid},
        new BeanListHandler(IdNameBean.class, new ConfIdNameRowProcessor())
    );

    final List<IdName> result = new ArrayList<IdName>();

    //noinspection unchecked
    result.addAll(idNames);

    return result;
  }

  public Conf findConfByName(final long expUid, long confUid) throws SQLException {
    final Conf conf = (Conf) getCtx().getQueryRunner().query(
        getCtx().getConn(),
        q.getQuery("conf.findByUId"),
        new Object[]{expUid, confUid},
        new BeanHandler(ConfBean.class, new ConfRowProcessor())
    );

    return conf;
  }

  public long insertConf(
      long expUid, String confName, String confDesc
  ) throws SQLException {
    final int updateCount = getCtx().getQueryRunner().update(
        getCtx().getConn(),
        q.getQuery("conf.insertNew"),
        new Object[]{
            expUid, confName, confDesc
        }
    );

    if (updateCount == 0) {
      throw new AssertionError("updateCount must be positive");
    }

    final Long lastUid = (Long) getCtx().getQueryRunner().query(
        getCtx().getConn(),
        q.getQuery("conf.findLastUid"),
        new ScalarHandler()
    );

    if (lastUid == null) {
      throw new AssertionError("lastUid must not be null");
    }

    return lastUid;
  }

  public Conf findConfById(long id) throws SQLException {
    final Conf conf = (Conf) getCtx().getQueryRunner().query(
        getCtx().getConn(),
        q.getQuery("conf.findByUid"),
        new Object[]{id},
        new BeanHandler(ConfBean.class, new ConfRowProcessor())
    );

    return conf;
  }

  public List<Parameter> listParametersForConf(final long confUid) throws SQLException {
    final List parameters = (List) getCtx().getQueryRunner().query(
        getCtx().getConn(),
        q.getQuery("par.findForConf"),
        new Object[]{confUid},
        new BeanListHandler(Parameter.class, new ParameterRowProcessor())
    );

    final List<Parameter> result = new ArrayList<Parameter>();

    //noinspection unchecked
    result.addAll(parameters);

    return result;
  }

  public int insertParam(
      long confUid, String name, String value,
      Parameter.Strategy strategy, Parameter.Strategy chainedStrategy, String desc
  ) throws SQLException {
    final int updateCount = getCtx().getQueryRunner().update(
        getCtx().getConn(),
        q.getQuery("par.insertNew"),
        new Object[]{
            confUid, name, value, strategy.toString(), chainedStrategy.toString(), desc
        }
    );

    return updateCount;
  }

  public SortedMap<Long, RunInfo> loadChainedRuns(final List<Long> chainedRunIds) {
    final SortedMap<Long, RunInfo> result = new TreeMap<Long, RunInfo>();
    if (chainedRunIds.isEmpty()) {
      return result;
    }

    final TreeSet<Long> pending = new TreeSet<Long>(chainedRunIds);
    final TreeSet<Long> loaded = new TreeSet<Long>();

    while (!pending.isEmpty()) {
      final long lastChainedRunId = pending.last();
      pending.remove(lastChainedRunId);
      if (loaded.contains(lastChainedRunId)) {
        continue;
      }

      try {
        final Run chainedRun = findRun(lastChainedRunId);

        if (chainedRun == null) {
          log.warn("no run for ID {}", lastChainedRunId);
          continue;
        }

        final ParamSetEnumerator chainedEnumerator = new ParamSetEnumerator();
        final long confId = chainedRun.getConfUid();

        boolean valid = true;
        if (confId >= 0) {
          final Conf chainedConf = findConfById(confId);
          if (chainedConf != null) {
            chainedEnumerator.load(listParametersForConf(chainedConf.getUid()), lastChainedRunId);
          } else {
            log.warn("failed to load conf {} of run {}", lastChainedRunId, confId);
            valid = false;
          }
        }

        if (valid) {
          result.put(lastChainedRunId, new RunInfo(chainedEnumerator, chainedRun));
          loaded.add(lastChainedRunId);

          pending.addAll(chainedRun.getChain());
        }
      } catch (SQLException e) {
        log.warn("failed to load run {}: {}", lastChainedRunId, Throwables.getRootCause(e).toString());
      }
    }

    return result;
  }

  public boolean updateRunPsetComplete(long runUid, final long psetComplete) throws SQLException {
    final int updateCount = getCtx().getQueryRunner().update(
        getCtx().getConn(),
        q.getQuery("run.updatePsetComplete"),
        new Object[]{
            psetComplete, runUid
        }
    );

    return updateCount > 0;
  }

  public boolean findCtxAttrNoLoad(long runId, long index, String path) throws SQLException {
    if (path == null) {
      return false;
    }

    final Number attrValue = (Number) getCtx().getQueryRunner().query(
        getCtx().getConn(),
        q.getQuery("ctx.countByRunSetPath"),
        new Object[]{runId, index, path, runId, index, path},
        new ScalarHandler()
    );

    return attrValue.longValue() > 0;
  }

  public SortedMap<Long, RunInfo> getChainedRuns(final String safeChainStr) {
    return loadChainedRuns(RunBean.parseChainSpec(safeChainStr));
  }
}

class ExperimentRowProcessor extends BasicRowProcessor {
  public Object toBean(ResultSet rs, Class type) throws SQLException {
    final Blob springXmlBlob = rs.getBlob("springXml");

    //  this exception jiggling really makes me sea-sick
    final String springXmlString;
    try {
      springXmlString = CharStreams.toString(new InputSupplier<Reader>() {
        public Reader getInput() throws IOException {
          try {
            return new InputStreamReader(springXmlBlob.getBinaryStream(), Charsets.UTF_8);
          } catch (SQLException e) {
            throw new IOException(e);
          }
        }
      });
    } catch (IOException e) {
      throw new SQLException(e);
    }

    final ExperimentBean experiment = new ExperimentBean(
        rs.getLong("uid"),
        rs.getString("id"),
        rs.getString("path"),
        rs.getString("depends"),
        rs.getString("desc"),
        rs.getLong("millis"),
        springXmlString
    );

    springXmlBlob.free();

    return experiment;
  }
}

class ConfIdNameRowProcessor extends BasicRowProcessor {
  public Object toBean(ResultSet rs, Class type) throws SQLException {
    final IdName idName = new IdNameBean(
        rs.getString("uid"),
        rs.getString("desc")
    );

    return idName;
  }
}

class ConfRowProcessor extends BasicRowProcessor {
  public Object toBean(ResultSet rs, Class type) throws SQLException {

    final ConfBean conf = new ConfBean(
        rs.getLong("uid"),
        rs.getLong("exp_uid"),
        rs.getString("name"),
        rs.getString("desc")
    );

    return conf;
  }
}

class ParameterRowProcessor extends BasicRowProcessor {
  public Object toBean(ResultSet rs, Class type) throws SQLException {
    final String name = rs.getString("name");
    final String valueSpec = rs.getString("value");
    final Parameter.Strategy strategy = Parameter.Strategy.fromString(rs.getString("strategy"));
    final Parameter.Strategy chainedStrategy = Parameter.Strategy.fromString(rs.getString("chainStrategy"));
    final String desc = rs.getString("desc");

    final Parameter param = Parameter.create(name, valueSpec);
    param.setStrategyCurrent(strategy);
    param.setStrategyChained(chainedStrategy);
    param.setDesc(desc);

    return param;
  }
}

class RunRowProcessor extends BasicRowProcessor {
  protected final boolean loadNames;

  RunRowProcessor(boolean loadNames) {
    this.loadNames = loadNames;
  }

  public Object toBean(ResultSet rs, Class type) throws SQLException {
    final RunBean run = new RunBean(
        rs.getLong("uid"),
        rs.getLong("exp_uid"),
        rs.getLong("conf_uid"),
        rs.getLong("millis"),
        rs.getLong("psetCount"),
        rs.getLong("psetComplete")
    );

    final String chainStr = rs.getString("chain");
    if (!Strings.isNullOrEmpty(chainStr)) {
      final List<Long> chain = Arrays.asList(Parse.longs(chainStr.split(" "), null));
      chain.removeAll(Collections.singletonList((Long) null));
      run.setChain(chain);
    }

    if (loadNames) {
      run.setConfDesc(rs.getString("conf_desc"));
      run.setExpDesc(rs.getString("exp_desc"));
    }

    return run;
  }
}

class CtxAttrRowProcessor extends BasicRowProcessor {
  protected final ValueDumper valueDumper;

  public CtxAttrRowProcessor(ValueDumper valueDumper) {
    this.valueDumper = valueDumper;
  }

  public Object toBean(ResultSet rs, Class type) throws SQLException {
    final String attrType = rs.getString("type");

    final String stringVal = rs.getString("val");

    if (valueDumper.isCNamePrimitive(attrType)) {
      return valueDumper.rsToPrimitive(attrType, stringVal);
    }

    final Class attrClass = valueDumper.rsToClass(attrType);

    Blob blob = null;
    BufferedReader bufferedReader = null;

    try {
      blob = rs.getBlob("content");
      bufferedReader = new BufferedReader(new InputStreamReader(blob.getBinaryStream(), "UTF-8"));

      return valueDumper.rsToDumpable(attrClass, bufferedReader);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("should not have happened", e);
    } finally {
      Closeables.closeQuietly(bufferedReader);
      if (blob != null) {
        blob.free();
      }
    }
  }
}