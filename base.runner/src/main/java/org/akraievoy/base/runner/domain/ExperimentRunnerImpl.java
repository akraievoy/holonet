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
import org.akraievoy.base.ObjArrays;
import org.akraievoy.base.runner.api.Context;
import org.akraievoy.base.runner.api.ContextInjectable;

import org.akraievoy.base.runner.domain.spring.StringXmlApplicationContext;
import org.akraievoy.base.runner.persist.ExperimentRegistry;
import org.akraievoy.base.runner.persist.RunnerDao;
import org.akraievoy.base.runner.vo.Conf;
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

  public ExperimentRunnerImpl(RunnerDao dao, ExperimentRegistry expDao) {
    this.dao = dao;
    this.expDao = expDao;
  }

  public void run(Experiment info, Conf conf, SortedMap<Long, RunInfo> chainedRuns) {
    final ParamSetEnumerator rootEnumerator = createEnumerator(conf);
    if (!validateRunChain(rootEnumerator, chainedRuns)) {
      return;
    }
    final ParamSetEnumerator widened = widen(rootEnumerator, chainedRuns);

    long runId;
    try {
      final long[] chainedRunIds = ObjArrays.unbox(chainedRuns.keySet().toArray(new Long[chainedRuns.size()]));
      runId = dao.insertRun(conf.getUid(), chainedRunIds, widened.getCount());
    } catch (SQLException e) {
      throw Throwables.propagate(e);
    }

    listener.onRunCreation(runId);
    runIterative(runId, info, rootEnumerator, widened, chainedRuns);
  }

  protected ParamSetEnumerator createEnumerator(Conf conf) {
    final ParamSetEnumerator paramSetEnumerator = new ParamSetEnumerator();

    if (conf != null) {
      try {
        paramSetEnumerator.load(dao.listParametersForConf(conf.getUid()), Long.MAX_VALUE);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    return paramSetEnumerator;
  }

  protected void runIterative(
      long runId, Experiment exp,
      ParamSetEnumerator root, ParamSetEnumerator widened,
      SortedMap<Long, RunInfo> runChain
  ) {
    final Context ctx = new Context(widened, dao, runId, runChain);

    do {
      runForPoses(runId, exp, widened, root, ctx);

      updateComplete(runId, widened.getIndex());
      listener.onPsetAdvance(runId, widened.getIndex());
    } while (widened.increment());
  }

  protected void updateComplete(long runId, final long index) {
    try {
      dao.updateRunPsetComplete(runId, index);
    } catch (SQLException e) {
      reportUpdateCompleteFailed(e);
    }
  }

  protected void reportUpdateCompleteFailed(SQLException e) {
    //	nothing to do here
  }

  protected void runForPoses(
      long runId, Experiment exp,
      ParamSetEnumerator widened, ParamSetEnumerator root,
      Context ctx
  ) {
    final ConfigurableApplicationContext context = new StringXmlApplicationContext(
        exp.getSpringXml(), false, applicationContext
    );

    final PropertyOverrideConfigurer configurer = new PropertyOverrideConfigurer();
    root.narrow(widened);
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
  }

  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = (ConfigurableApplicationContext) applicationContext;
  }

  protected boolean validateRunChain(ParamSetEnumerator root, SortedMap<Long, RunInfo> chainedRuns) {
    for (final RunInfo run : chainedRuns.values()) {
      final Parameter collision = root.findCollision(run.getEnumerator());
      if (collision != null) {
        reportRootCollision(run, collision);
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
          reportChainedCollision(chainedRuns.get(idI), chainedRuns.get(idJ), collision);
          return false;
        }
      }
    }

    return true;
  }

  protected void reportChainedCollision(RunInfo runI, RunInfo runJ, Parameter collision) {
    //	nothing to do here
  }

  protected void reportRootCollision(RunInfo run, Parameter collision) {
    //	nothing to do here
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
}

