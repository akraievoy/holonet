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

import org.akraievoy.base.runner.domain.spring.StringXmlApplicationContext;
//  FIXME this import smells
import org.akraievoy.base.runner.swing.BatchTableModel;
import org.akraievoy.base.soft.Soft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.akraievoy.base.runner.persist.RunnerDao;
import org.akraievoy.base.runner.vo.Conf;
import org.akraievoy.base.runner.vo.Experiment;
import org.akraievoy.db.CouchDb;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

import javax.annotation.Nullable;
import javax.swing.*;

/**
 * Run several experiments, some of them being chained with previous ones,
 * and also issue assertions over results we see in the database after some of
 * experiments.
 */
public interface BatchRunner {
  static interface ValueProcessor {
    void processValue(Object attr, String attrSpec);
  }

  interface Callback {
    void batchAdvanced(
        final BatchTableModel.BatchDef def,
        final int pos,
        final int count,
        boolean success
    );

    void batchChanged(
        @Nullable BatchTableModel.BatchDef def,
        @Nullable Batch batch,
        boolean success);

    Callback NOOP = new Callback() {
      public void batchAdvanced(
          BatchTableModel.BatchDef def,
          int pos,
          int count,
          boolean success
      ) {
        //  nothing to do here
      }

      public void batchChanged(
          @Nullable BatchTableModel.BatchDef def,
          @Nullable Batch batch,
          boolean success) {
        //  nothing to do here
      }
    };
  }
  
  static class Batch {
    private final List<BatchComponent> components =
        new ArrayList<BatchComponent>();
    private final Map<String, Long> prevRuns = new TreeMap<String, Long>();
    private Callback callback = Callback.NOOP;

    public Batch(final List<BatchComponent> myComponents) {
      this.components.addAll(myComponents);
    }

    public void setCallback(Callback callback) {
      this.callback = callback;
    }

    public void registerRun(Long runId, String path) {
      prevRuns.put(path, runId);
    }

    public Long resolve(String expPath) {
      final Long runId = prevRuns.get(expPath);
      if (runId == null) {
        throw new IllegalStateException(
            "experiment not run before assert: '" + expPath + "'"
        );
      }
      return runId;
    }

    public ArrayList<Long> resolvePaths(String[] chainedPaths, String path) {
      final ArrayList<Long> chainedRunIds = new ArrayList<Long>();
      for (String chainedPath : chainedPaths) {
        final Long prevRunId = getPrevRuns().get(chainedPath);
        if (prevRunId == null) {
          throw new IllegalStateException(
              "no prev. run: " +
                  "'" + chainedPath + "'->'" + path + "' " +
                  "(chained->path)"
          );
        }
        chainedRunIds.add(prevRunId);
      }
      return chainedRunIds;
    }

    public void run(
        Ctx ctx, final BatchTableModel.BatchDef def
    ) {
      try {
        final int count = components.size();
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            callback.batchAdvanced(def, 0, count, true);
          }
        });
        for (int pos = 0; pos < count; pos++) {
          BatchComponent comp = components.get(pos);
          boolean success = false;
          try {
            comp.run(ctx, this);
            success = true;
          } finally {
            //  I have to finalize the values before passing
            //    them to to callback
            final int callbackPos = pos + 1;
            final boolean callbackSuccess = success;
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                callback.batchAdvanced(
                    def, callbackPos, count, callbackSuccess
                );
              }
            });
          }
        }
      } finally {
        prevRuns.clear();
      }
    }

    public Map<String, Long> getPrevRuns() {
      return Collections.unmodifiableMap(prevRuns);
    }

    public List<BatchComponent> getComponents() {
      return Collections.unmodifiableList(components);
    }
  }

  static interface Ctx {
    CouchDb getCouch();
    RunnerDao getRunnerDao();
    ExperimentRunner getExperimentRunner();
  }

  static interface BatchComponent {
    public void run(Ctx ctx, Batch batch);
  }

  Logger log = LoggerFactory.getLogger(BatchRunner.class);

  void setCallback(Callback callback);

  Batch getRunningBatch();

  void runBatch(
      ExecutorService executor,
      BatchTableModel.BatchDef batchDef
  );

  static class Impl
      implements BatchRunner, Ctx, ApplicationContextAware {

    protected AtomicReference<Batch> runningBatch =
        new AtomicReference<Batch>();

    public Impl() {
      //  nothing to do here
    }

    protected ConfigurableApplicationContext applicationContext;
    public void setApplicationContext(ApplicationContext appContext)
        throws BeansException {
      this.applicationContext = (ConfigurableApplicationContext) appContext;
    }

    protected CouchDb couch;
    public void setCouch(CouchDb couch) { this.couch = couch; }

    protected RunnerDao runnerDao;
    public void setRunnerDao(RunnerDao runnerDao) { this.runnerDao = runnerDao; }

    protected ExperimentRunner experimentRunner;
    public void setExperimentRunner(
        ExperimentRunner experimentRunner
    ) {
      this.experimentRunner = experimentRunner;
    }

    protected Callback callback = Callback.NOOP;
    public void setCallback(Callback callback) { this.callback = callback; }

    public void start() {
      if (getExperimentRunner() == null) {
        throw new IllegalStateException("experimentRunner should be wired in");
      }
/*
      if (getCouch() == null) {
        throw new IllegalStateException("couch should be wired in");
      }
*/
      if (getRunnerDao() == null) {
        throw new IllegalStateException("runnerDao should be wired in");
      }
    }

    public Batch getRunningBatch() {
      return runningBatch.get();
    }

    public void runBatch(
        final ExecutorService executor,
        final BatchTableModel.BatchDef batchDef
    ) {
      executor.submit(new Runnable() {
        public void run() {
          boolean success = false;
          try {
            final StringXmlApplicationContext context =
                new StringXmlApplicationContext(
                    batchDef.getXml(), false, applicationContext
                );
            context.refresh();
            final Batch batch = (Batch) context.getBean("batch");
            batch.setCallback(callback);
            runningBatch.set(batch);
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                callback.batchChanged(batchDef, batch, true);
              }
            });

            batch.run(BatchRunner.Impl.this, batchDef);
            success = true;
          } catch (Exception e) {
            log.warn("batch failed", e);
          } finally {
            runningBatch.set(null);
            //  have to finalize this for the inner class
            final boolean callbackSuccess = success;
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                callback.batchChanged(null, null, callbackSuccess);
              }
            });
          }
        }
      });
    }

    public CouchDb getCouch() {
      return couch;
    }

    public RunnerDao getRunnerDao() {
      return runnerDao;
    }

    public ExperimentRunner getExperimentRunner() {
      return experimentRunner;
    }
  }

  public static class ComponentExperiment implements BatchComponent {
    private final String path;
    private final String confPath;
    private final String[] chainedPaths;
    
    public ComponentExperiment(
        final String path,
        final String confPath
    ) {
      this(path, confPath, new String[]{});
    }

    public ComponentExperiment(
        final String path,
        final String confPath,
        final String[] chainedPaths
    ) {
      this.path = path;
      this.confPath = confPath;
      this.chainedPaths = chainedPaths;
    }

    public void run(Ctx ctx, final Batch batch) {
      final Experiment expByPath;
      final long expUid;
      try {
        expByPath = ctx.getRunnerDao().findExperimentByPath(path);
        if (expByPath == null) {
          throw new IllegalStateException(
              "experiment not found: " +
                  "'"+ path +"' " +
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
        confByPath = ctx.getRunnerDao().findConfByPath(expUid, confPath);
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


      final ArrayList<Long> chainedRunIds =
          batch.resolvePaths(chainedPaths, path);

      final Long runId = ctx.getExperimentRunner().run(
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

      batch.registerRun(runId, path);
    }
  }

  public static class ComponentNumberPrint extends ComponentValue {
    public ComponentNumberPrint(String expPath, long index, String ctxPath) {
      super(expPath, index, ctxPath);
    }

    public void processValue(Object attr, String attrSpec) {
      if (!(attr instanceof Number)) {
        throw new IllegalStateException(
            "should be a Number: " + attrSpec
        );
      }

      log.info("retrieved " + ((Number) attr).doubleValue() + ": " + attrSpec);
    }
  }

  public static class ComponentNumberEqual extends ComponentValue {
    private final Double value;

    public ComponentNumberEqual(String expPath, long index, String ctxPath, Double value) {
      super(expPath, index, ctxPath);
      this.value = value;
    }

    public void processValue(Object attr, String attrSpec) {
      if (!(attr instanceof Number)) {
        throw new IllegalStateException(
            "should be a Number: " + attrSpec
        );
      }

      final Number numAttr = (Number) attr;
      if (!(Soft.MICRO.equal(numAttr.doubleValue(), value))) {
        throw new IllegalStateException(
            "expected: " + value + " actual: " + numAttr.doubleValue() +
                ": " + attrSpec
        );
      }
    }
  }

  public static class ComponentValue implements BatchComponent {
    private final String expPath;
    private final long index;
    private final String ctxPath;

    public ComponentValue(String expPath, long index, String ctxPath) {
      this.expPath = expPath;
      this.index = index;
      this.ctxPath = ctxPath;
    }

    public void run(Ctx ctx, final Batch batch) {
      final Long runId = batch.resolve(expPath);
      final String attrSpec = attrSpec(runId, expPath, ctxPath);
      try {
        final Object attr = ctx.getRunnerDao().findCtxAttr(runId, index, ctxPath);

        if (attr == null) {
          throw new IllegalStateException(
              "should be defined: " + attrSpec
          );
        }

        processValue(attr, attrSpec);
      } catch (SQLException e) {
        throw new IllegalStateException(
            "attr load failed: " + attrSpec
        );
      }
    }

    protected void processValue(Object attr, String attrSpec) {
      //  nothing to do here
    }

    protected static String attrSpec(
        Long runId, String expPath, String ctxPath
    ) {
      return "'" + expPath + "'/(" + runId + ")/'" + ctxPath + "'";
    }
  }
}
