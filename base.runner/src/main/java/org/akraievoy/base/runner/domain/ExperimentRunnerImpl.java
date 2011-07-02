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
import org.akraievoy.base.ref.Ref;
import org.akraievoy.base.ref.RefSimple;
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
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.*;

public class ExperimentRunnerImpl implements ExperimentRunner, ApplicationContextAware {
  protected ConfigurableApplicationContext applicationContext;

  protected final RunnerDao dao;
  protected final ExperimentRegistry expDao;

  protected RunStateListener listener = RunStateListener.NOOP;
  protected long startMillis;

  private final ThreadFactory threadFactory = new ThreadFactory() {
    private int threadNum = 0;
    public Thread newThread(Runnable target) {
      final Thread newThread = new Thread(target, "ExperimentRunner #" + threadNum);
      newThread.setPriority(Thread.MIN_PRIORITY);
      newThread.setDaemon(true);
      threadNum += 1;

      return newThread;
    }
  };
  private final int processors = Runtime.getRuntime().availableProcessors();
  private final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(processors);
  private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
      processors, processors,
      Long.MAX_VALUE, TimeUnit.SECONDS,
      queue, threadFactory
  );
  private static final int SCHEDULE_MILLIS = 1000;

  public ExperimentRunnerImpl(RunnerDao dao, ExperimentRegistry expDao) {
    this.dao = dao;
    this.expDao = expDao;
  }

  public void run(Experiment info, final long confUid, final List<Long> chainedRunIds) {
    RunContextImpl runContext = loadRunContext(null, confUid, chainedRunIds);
    if (!runContext.isValid()) {
      return;
    }

    try {
      runContext.setRunId(dao.insertRun(
          confUid,
          runContext.getChainedRunIds(),
          runContext.getWideParams().getCount()
      ));
    } catch (SQLException e) {
      throw Throwables.propagate(e);
    }

    listener.onRunCreation(runContext.getRunId());
    runIterative(info, runContext);
  }

  public RunContextImpl loadRunContext(final Long runUid, long confUid, final List<Long> chainedRunIds) {
    final RunContextImpl runContext = new RunContextImpl(confUid, chainedRunIds).invoke();
    if (runUid != null) {
      runContext.setRunId(runUid);
    }
    return runContext;
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

  protected void runIterative(final Experiment exp, final RunContext runContext) {
    log.info(
        "Starting {} with runId = {} ({} cores)",
        new Object[] {exp.getDesc(), runContext.getRunId(), processors}
    );
    startMillis = System.currentTimeMillis();

    try {
      do {
        runParallel(exp, runContext);
      } while (runContext.getWideParams().increment(true, false));

      log.info("Completed experiment {}, runId = {}", exp.getDesc(), runContext.getRunId());
    } catch (Exception e) {
      log.error("Failed {} ", Throwables.getRootCause(e).toString());
      throw Throwables.propagate(e);
    }
  }

  protected void runParallel(final Experiment exp, final RunContext runContext) throws InterruptedException {
    final Ref<Long> indexRef = new RefSimple<Long>(null);

    do {
      while (queue.size() >= processors) {
        Thread.sleep(SCHEDULE_MILLIS);
      }
      final RunContext runContextLocal = runContext.dupe();
      executor.execute(new Runnable() {
        public void run() {
          runForPoses(exp, new Context(runContextLocal, dao));

          final long index = runContextLocal.getWideParams().getIndex(true);
          if (indexRef.getValue() == null || indexRef.getValue() < index) {
            indexRef.setValue(index);
            updateComplete(runContext.getRunId(), index);
            listener.onPsetAdvance(runContext.getRunId(), index);
          }
        }
      });
    } while (runContext.getWideParams().increment(false, true));

    while (!queue.isEmpty() || executor.getActiveCount() > 0) {
      Thread.sleep(SCHEDULE_MILLIS);
    }
  }

  protected void updateComplete(long runId, final long index) {
    try {
      dao.updateRunPsetComplete(runId, index);
    } catch (SQLException e) {
      ExperimentRunner.log.warn("failed to update complete marker: {}", Throwables.getRootCause(e).toString());
    }
  }

  protected void runForPoses(Experiment exp, Context ctx) {
    final long paramSetCount = ctx.getRunContext().getWideParams().getCount();
    final Runtime runtime = Runtime.getRuntime();

    final ConfigurableApplicationContext context = new StringXmlApplicationContext(
        exp.getSpringXml(), false, applicationContext
    );

    final PropertyOverrideConfigurer configurer = new PropertyOverrideConfigurer();
    //  LATER it's better to pass params via chain, not DB
    ctx.getRunContext().getRootParams().narrow(ctx.getRunContext().getWideParams());
    configurer.setProperties(ctx.getRunContext().getRootParams().getProperties());
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
      final long index = ctx.getRunContext().getWideParams().getIndex(true);
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

  public class RunContextImpl implements RunContext {
    private final long confUid;

    public RunContextImpl(long confUid, final List<Long> chainedRunIds) {
      this.confUid = confUid;
      this.chainedRunIds = chainedRunIds;
    }

    private final List<Long> chainedRunIds;
    public long[] getChainedRunIds() {
      return ObjArrays.unbox(getChainedRuns().keySet().toArray(new Long[getChainedRuns().size()]));
    }

    private SortedMap<Long, RunInfo> chainedRuns;
    public SortedMap<Long, RunInfo> getChainedRuns() { return chainedRuns; }

    private ParamSetEnumerator rootParams;
    public ParamSetEnumerator getRootParams() { return rootParams; }

    private ParamSetEnumerator wideParams;
    public ParamSetEnumerator getWideParams() { return wideParams; }

    private boolean valid = false;
    public boolean isValid() { return valid; }

    private long runId;
    public void setRunId(long runId) { this.runId = runId; }
    public long getRunId() { return runId; }

    public RunContext dupe() {
      final RunContextImpl dupe = new RunContextImpl(confUid, chainedRunIds);

      if (chainedRuns != null) {
        dupe.chainedRuns = new TreeMap<Long, RunInfo>(chainedRuns);
      }
      dupe.rootParams = rootParams != null ? rootParams.dupe(null) : null;
      dupe.wideParams = wideParams != null ? wideParams.dupe(null) : null;
      dupe.valid = valid;
      dupe.runId = runId;

      return dupe;
    }

    public RunContextImpl invoke() {
      chainedRuns = dao.loadChainedRuns(chainedRunIds);
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

