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
import org.akraievoy.base.Parse;
import org.akraievoy.base.runner.api.ContextInjectable;
import org.akraievoy.base.runner.api.ParamSetEnumerator;
import org.akraievoy.base.runner.api.StandaloneIterator;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExperimentRunnerImpl implements ExperimentRunner, ApplicationContextAware {
  protected ConfigurableApplicationContext applicationContext;

  protected final RunnerDao dao;
  protected final ExperimentRegistry expDao;

  protected RunStateListener listener = RunStateListener.NOOP;

  public ExperimentRunnerImpl(RunnerDao dao, ExperimentRegistry expDao) {
    this.dao = dao;
    this.expDao = expDao;
  }

  public void run(Experiment info, Conf conf, RunInfo[] chainedRuns) {
    final ParamSetEnumeratorBase rootEnumerator = createEnumerator(conf);
    if (!validateRunChain(rootEnumerator, chainedRuns)) {
      return;
    }
    final ParamSetEnumerator widened = widen(rootEnumerator, chainedRuns);

    long runId;
    try {
      runId = dao.insertRun(conf.getUid(), RunInfo.getRunIds(chainedRuns), widened.getCount());
    } catch (SQLException e) {
      throw Throwables.propagate(e);
    }

    listener.onRunCreation(runId);
    runIterative(runId, info, rootEnumerator, widened, chainedRuns);
  }

  protected ParamSetEnumeratorBase createEnumerator(Conf conf) {
    final ParamSetEnumeratorBase paramSetEnumerator = new ParamSetEnumeratorBase();

    if (conf != null) {
      try {
        paramSetEnumerator.load(dao.listParametersForConf(conf.getUid()));
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    return paramSetEnumerator;
  }

  protected void runIterative(long runId, Experiment exp, ParamSetEnumerator root, ParamSetEnumerator widened, RunInfo[] runChain) {
    final ContextLogging ctx = new ContextLogging(runId, dao, widened, runChain);

    List<String> iteratedParamNames;
    do {
      iteratedParamNames = runForPoses(runId, exp, widened, root, ctx);

      updateComplete(runId, widened.getIndex(iteratedParamNames));
      listener.onPsetAdvance(runId, widened.getIndex());
    } while (widened.increment(iteratedParamNames));
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

  protected List<String> runForPoses(long runId, Experiment exp, ParamSetEnumerator widened, ParamSetEnumerator root, final ContextLogging ctx) {
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

      if (main instanceof StandaloneIterator) {
        return ((StandaloneIterator) main).getIteratedParamNames();
      } else {
        return Collections.emptyList();
      }
    } finally {
      context.close();
    }
  }

  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = (ConfigurableApplicationContext) applicationContext;
  }

  protected boolean validateRunChain(ParamSetEnumeratorBase root, RunInfo[] chainedRuns) {
    for (final RunInfo run : chainedRuns) {
      final Parameter collision = root.findCollision((ParamSetEnumeratorBase) run.getEnumerator());
      if (collision != null) {
        reportRootCollision(run, collision);
        return false;
      }
    }

    for (int i = 0, chainedRunsLength = chainedRuns.length; i < chainedRunsLength; i++) {
      final RunInfo runI = chainedRuns[i];
      final ParamSetEnumeratorBase enumI = (ParamSetEnumeratorBase) runI.getEnumerator();
      for (int j = 0; j < i; j++) {
        final RunInfo runJ = chainedRuns[j];
        final ParamSetEnumeratorBase enumJ = (ParamSetEnumeratorBase) runJ.getEnumerator();
        final Parameter collision = enumI.findCollision(enumJ);

        if (collision != null) {
          reportChainedCollision(runI, runJ, collision);
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

  protected ParamSetEnumerator widen(ParamSetEnumerator root, RunInfo[] runChain) {
    final ParamSetEnumerator widenedPse = root.dupe(true);

    for (RunInfo chained : runChain) {
      widenedPse.widen(chained.getEnumerator());
    }

    //	we need to recreate same parameter spaces for all chained experiments
    for (RunInfo widenee : runChain) {
      final Long[] chainedRefs = Parse.longs(widenee.getRun().getChain().split(" "), -1L);
      for (Long chainedRef : chainedRefs) {
        for (RunInfo widener : runChain) {
          if (widener.getRun().getUid() == chainedRef) {
            widenee.getEnumerator().widen(widener.getEnumerator());
            break;
          }
        }
      }
    }

    return widenedPse;
  }

  public void setListener(RunStateListener listener) {
    this.listener = listener;
  }
}

