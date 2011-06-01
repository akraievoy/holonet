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
import org.akraievoy.base.Die;
import org.akraievoy.base.Format;
import org.akraievoy.base.ObjArrays;
import org.akraievoy.base.runner.api.Context;
import org.akraievoy.base.runner.api.ContextInjectable;

import org.akraievoy.base.runner.domain.spring.StringXmlApplicationContext;
import org.akraievoy.base.runner.persist.ExperimentRegistry;
import org.akraievoy.base.runner.persist.RunnerDao;
import org.akraievoy.base.runner.vo.Experiment;
import org.akraievoy.base.runner.vo.Parameter;
import org.akraievoy.base.runner.vo.RunInfo;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.PropertyOverrideConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

import java.sql.SQLException;
import java.util.Map;
import java.util.SortedMap;

public class ExperimentRunnerImpl implements ExperimentRunner, ApplicationContextAware {
  protected ConfigurableApplicationContext applicationContext;

  protected final RunnerDao dao;
  protected final ExperimentRegistry expDao;

  protected RunStateListener listener = RunStateListener.NOOP;
  protected long startMillis;

  public ExperimentRunnerImpl(RunnerDao dao, ExperimentRegistry expDao) {
    this.dao = dao;
    this.expDao = expDao;
  }

  public void run(Experiment info, final long confUid, String safeChainSpec) {
    RunContext runContext = new RunContext(confUid, safeChainSpec).invoke();
    if (!runContext.isValid()) {
      return;
    }

    ParamSetEnumerator widened = runContext.getWideParams();

    long runId;
    try {
      final long[] chainedRunIds = runContext.getChainedRunIds();
      runId = dao.insertRun(confUid, chainedRunIds, runContext.getWideParams().getCount());
    } catch (SQLException e) {
      throw Throwables.propagate(e);
    }

    listener.onRunCreation(runId);
    runIterative(runId, info, runContext);
  }

  protected ParamSetEnumerator createEnumerator(final Long confUid) {
    final ParamSetEnumerator paramSetEnumerator = new ParamSetEnumerator();

    if (confUid != null) {
      try {
        paramSetEnumerator.load(dao.listParametersForConf(confUid), Long.MAX_VALUE);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    return paramSetEnumerator;
  }
  protected void runIterative(long runId, Experiment exp, RunContext runContext) {
    log.info("Starting {} with runId = {}", exp.getDesc(), runId);
    startMillis = System.currentTimeMillis();

    try {
      final Context ctx = new Context(runContext.getWideParams(), dao, runId, runContext.getChainedRuns());

      do {
        runForPoses(exp, runContext.getWideParams(), runContext.getRootParams(), ctx);

        updateComplete(runId, runContext.getWideParams().getIndex(true));
        listener.onPsetAdvance(runId, runContext.getWideParams().getIndex(true));
      } while (runContext.getWideParams().increment());

      log.info("Completed experiment {}, runId = {}", exp.getDesc(), runId);
    } catch (Exception e) {
      log.error("Failed {} ", Throwables.getRootCause(e).toString());
      throw Throwables.propagate(e);
    }
  }

  protected void updateComplete(long runId, final long index) {
    try {
      dao.updateRunPsetComplete(runId, index);
    } catch (SQLException e) {
      ExperimentRunner.log.warn("failed to update complete marker: {}", Throwables.getRootCause(e).toString());
    }
  }

  protected void runForPoses(
      Experiment exp, ParamSetEnumerator widened, ParamSetEnumerator root, Context ctx
  ) {
    final long paramSetCount = widened.getCount();
    final Runtime runtime = Runtime.getRuntime();

    final ConfigurableApplicationContext context = new StringXmlApplicationContext(
        exp.getSpringXml(), false, applicationContext
    );

    final PropertyOverrideConfigurer configurer = new PropertyOverrideConfigurer();
    root.narrow(widened); //  LATER it's better to pass params via chain, not DB
    configurer.setProperties(root.getProperties());
    context.addBeanFactoryPostProcessor(configurer);

    context.refresh();

    final Map map = context.getBeansOfType(ContextInjectable.class);
    for (Object key : map.keySet()) {
      final ContextInjectable injectable = (ContextInjectable) map.get(key);
      injectable.setCtx(ctx);
    }

    try {
      final Runnable main = (Runnable) context.getBean("main");
      Die.ifNull("main", main);

      main.run();
    } finally {
      context.close();
    }

    final long totalMem = runtime.totalMemory();
    final long freeMem = runtime.freeMemory();
    if (paramSetCount > 0) {
      final long index = widened.getIndex(true);
      log.info("Parameter set {} / {}, mem used {} / {}, ETA: {} ",
          new Object[]{
              index, paramSetCount,
              Format.formatMem(totalMem - freeMem), Format.formatMem(totalMem),
              Format.formatDuration((System.currentTimeMillis() - startMillis) * (paramSetCount - index) / index)
          });
    } else {
      log.info("Mem used {} / {} ", Format.formatMem(totalMem - freeMem), Format.formatMem(totalMem));
    }
  }

  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = (ConfigurableApplicationContext) applicationContext;
  }

  protected boolean validateRunChain(ParamSetEnumerator root, SortedMap<Long, RunInfo> chainedRuns) {
    for (final RunInfo run : chainedRuns.values()) {
      final Parameter collision = root.findCollision(run.getEnumerator());
      if (collision != null) {
        log.warn("param set for run {} collides with root at parameter {}", run.getRun(), collision.getName());
        return false;
      }
    }

    for (long idI : chainedRuns.keySet()) {
      final ParamSetEnumerator enumI = chainedRuns.get(idI).getEnumerator();
      for (long idJ : chainedRuns.keySet()) {
        if (idI <= idJ) {
          continue;
        }
        final ParamSetEnumerator enumJ = chainedRuns.get(idJ).getEnumerator();

        final Parameter collision = enumI.findCollision(enumJ);
        if (collision != null) {
          log.warn(
              "param set for run {} collides with param set for run {} at parameter {}",
              new Object[]{chainedRuns.get(idI), chainedRuns.get(idJ), collision.getName()}
          );
          return false;
        }
      }
    }

    return true;
  }

  protected ParamSetEnumerator widen(ParamSetEnumerator root, SortedMap<Long, RunInfo> runChain) {
    final ParamSetEnumerator current = root.dupe(null).restart();

    for (RunInfo chained : runChain.values()) {
      current.widen(chained.getEnumerator());
    }

    //	we need to recreate same parameter spaces for all chained experiments
    for (RunInfo widenee : runChain.values()) {
      for (Long chainedRef : widenee.getRun().getChain()) {
        widenee.getEnumerator().widen(runChain.get(chainedRef).getEnumerator());
      }
    }

    return current;
  }

  public void setListener(RunStateListener listener) {
    this.listener = listener;
  }

  public class RunContext {
    private final long confUid;
    private final String safeChainSpec;
    private SortedMap<Long, RunInfo> chainedRuns;
    private ParamSetEnumerator rootParams;
    private ParamSetEnumerator wideParams;
    private boolean valid = false;

    public RunContext(long confUid, String safeChainSpec) {
      this.confUid = confUid;
      this.safeChainSpec = safeChainSpec;
    }

    public SortedMap<Long, RunInfo> getChainedRuns() {
      return chainedRuns;
    }

    public ParamSetEnumerator getRootParams() {
      return rootParams;
    }

    public ParamSetEnumerator getWideParams() {
      return wideParams;
    }

    public boolean isValid() {
      return valid;
    }

    protected long[] getChainedRunIds() {
      return ObjArrays.unbox(getChainedRuns().keySet().toArray(new Long[getChainedRuns().size()]));
    }

    public RunContext invoke() {
      chainedRuns = dao.getChainedRuns(safeChainSpec);
      rootParams = createEnumerator(confUid);

      if (!validateRunChain(rootParams, chainedRuns)) {
        valid = false;
        return this;
      }

      wideParams = widen(rootParams, chainedRuns);
      valid = true;
      return this;
    }
  }
}

