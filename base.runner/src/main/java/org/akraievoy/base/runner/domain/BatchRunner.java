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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.akraievoy.base.runner.persist.RunnerDao;
import org.akraievoy.base.runner.vo.Conf;
import org.akraievoy.base.runner.vo.Experiment;
import org.akraievoy.base.soft.Soft;
import org.akraievoy.db.CouchDb;

/**
 * Run several experiments, some of them being chained with previous ones,
 * and also issue assertions over results we see in the database after some of
 * experiments.
 */
public class BatchRunner implements ApplicationContextAware {
  public static interface ValueProcessor {
    void processValue(Object attr, String attrSpec);
  }

  public static final Logger log = LoggerFactory.getLogger(BatchRunner.class);

  //  those are set at bean initialization
  protected ApplicationContext applicationContext;
  protected CouchDb couch;
  protected RunnerDao runnerDao;
  protected ExperimentRunner experimentRunner;

  //  those are set during the batch run itself
  protected final Map<String, Long> prevRuns = new TreeMap<String, Long>();


  public BatchRunner() {
    //  nothing to do here
  }

  public void setApplicationContext(
      ApplicationContext applicationContext
  ) throws BeansException {
    //  this context should have at least those
    //    beans which are defined in the file
    //      runner-main.xml
    this.applicationContext = applicationContext;

    //  so we may go a step further and resolve any beans we would need
    //    for the tasks of this batch runner we implement here

    //  persistence
    couch = (CouchDb) applicationContext.getBean(
        "couchDb",
        CouchDb.class
    );
    runnerDao = (RunnerDao) applicationContext.getBean(
        "runnerDao"
    );

    //  the runner bean
    experimentRunner = (ExperimentRunner) applicationContext.getBean(
        "experimentRunner"
    );
  }

  public Runnable experiment(
      final String path,
      final String confPath,
      final String[] chainedPaths
  ) {
    return new Runnable() {
      public void run() {
        final Experiment expByPath;
        final long expUid;
        try {
          expByPath = runnerDao.findExperimentByPath(path);
          if (expByPath == null) {
            throw new IllegalStateException(
                "experiment not found: " +
                    "'"+path +"' " +
                    "(path)"
            );
          }
          expUid = expByPath.getUid();
        } catch (SQLException e) {
          throw new RuntimeException(
              "find experiment failed: " +
                  "'" + path + "' " +
                  "(path)", e
          );
        }

        final Conf confByPath;
        try {
          confByPath = runnerDao.findConfByPath(expUid, confPath);
        } catch (SQLException e) {
          throw new RuntimeException(
              "find config failed: " +
                  "'" + path + "'/'" + confPath + "' " +
                  "(path/confPath)",
              e
          );
        }

        if (confByPath == null) {
          throw new IllegalStateException(
              "conf not found: " +
                  "'"+ path +"'/'" + confPath + "' " +
                  "(path/confPath)"
          );
        }


        final ArrayList<Long> chainedRunIds = new ArrayList<Long>();

        for (String chainedPath : chainedPaths) {
          final Long prevRunId = prevRuns.get(chainedPath);
          if (prevRunId == null) {
            throw new IllegalStateException(
                "no prev. run: " +
                    "'" + chainedPath + "'->'" + path + "' " +
                    "(chained->path)"
            );
          }
          chainedRunIds.add(prevRunId);
        }

        final Long runId = experimentRunner.run(
            expByPath,
            confByPath.getUid(),
            chainedRunIds
        );

        if (runId == null) {
          throw new IllegalStateException(
              "runner bean refused to run: " +
                  "'"+ path +"'/'" + confPath + "' " +
                  "(path/confPath)"
          );
        }

        prevRuns.put(path, runId);
      }
    };
  }

  public Runnable chain(
      final List<Runnable> runnables
  ) {
    return new Runnable() {
      public void run() {
        try {
          for (Runnable runnable : runnables) {
            runnable.run();
          }
        } finally {
          prevRuns.clear();
        }
      }
    };
  }

  public Runnable assertPresent(
      final String expPath,
      final long index,
      final String ctxPath
  ) {
    return new Runnable() {
      public void run() {
        final Long runId = prevRuns.get(expPath);
        if (runId == null) {
          throw new IllegalStateException(
              "experiment not run before assert: '" + expPath + "'"
          );
        }

        final String attrSpec = attrSpec(runId, expPath, ctxPath);
        try {
          if (!runnerDao.findCtxAttrNoLoad(runId, index, ctxPath)) {
            throw new IllegalStateException(
                "attr should be present: " + attrSpec
            );
          }
        } catch (SQLException e) {
          throw new IllegalStateException(
              "attr presence check failed: " + attrSpec
          );
        }
      }
    };
  }

  public Runnable process(
      final String expPath,
      final long index,
      final String ctxPath, final ValueProcessor processor
  ) {
    return new Runnable() {
      public void run() {
        final Long runId = prevRuns.get(expPath);
        if (runId == null) {
          throw new IllegalStateException(
              "experiment not run before assert: " +
                  "'" + expPath + "'"
          );
        }

        final String attrSpec = attrSpec(runId, expPath, ctxPath);
        try {
          final Object attr = runnerDao.findCtxAttr(runId, index, ctxPath);

          if (attr == null) {
            throw new IllegalStateException(
                "should be defined: " + attrSpec
            );
          }

          processor.processValue(attr, attrSpec);
        } catch (SQLException e) {
          throw new IllegalStateException(
              "attr load failed: " + attrSpec
          );
        }
      }
    };
  }

  public Runnable printNumber(
      final String expPath,
      final long index,
      final String ctxPath
  ) {
    return process(expPath, index, ctxPath, new ValueProcessor() {
      public void processValue(Object attr, String attrSpec) {
        if (!(attr instanceof Number)) {
          throw new IllegalStateException(
              "should be a Number: " + attrSpec
          );
        }

        log.info("retrieved " + ((Number) attr).doubleValue() + ": " + attrSpec);
      }
    });
  }

  public Runnable assertNumberEqual(
      final String expPath,
      final long index,
      final String ctxPath,
      final Double value
  ) {
    return process(expPath, index, ctxPath, new ValueProcessor() {
      public void processValue(Object attr, String attrSpec) {
        if (!(attr instanceof Number)) {
          throw new IllegalStateException(
              "should be a Number: " + attrSpec
          );
        }

        final Number numAttr = (Number) attr;
        if (!(Soft.MICRO.equal(numAttr.doubleValue(), value))) {
          throw new IllegalStateException(
              "expected: " + value + " actual: " + numAttr.doubleValue() + ": " + attrSpec
          );
        }
      }
    });
  }

  protected static String attrSpec(Long runId, String expPath, String ctxPath) {
    return "'" + expPath + "'/(" + runId + ")/'" + ctxPath + "'";
  }
}
