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
import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import org.akraievoy.base.runner.vo.Experiment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.SortedMap;
import java.util.TreeMap;

public class ImportRunnable implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(ImportRunnable.class);

  protected static final String EXP_EXT = ".xml";

  protected final RunnerDao dao;

  protected String basePath = "data/import";
  protected String registryResource = "registry.properties";
  protected Runnable afterImport = null;

  public ImportRunnable(RunnerDao dao) {
    this.dao = dao;
  }

  public void setRegistryResource(String registryResource) {
    this.registryResource = registryResource;
  }

  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }

  public Runnable getAfterImport() {
    return afterImport;
  }

  public void setAfterImport(Runnable afterImport) {
    this.afterImport = afterImport;
  }

  public void run() {
    try {
      doImport();
    } catch (Throwable t) {
      log.error("unexpected error while serializing: {}", Throwables.getRootCause(t).toString());
      log.debug("[detailed trace]", t);
    } finally {
      if (afterImport != null) {
        afterImport.run();
      }
    }
  }

  protected void doImport() {
    final String[] expPaths = listExperimentPaths();

    for (String expPath : expPaths) {
      final File sourceFile = new File(basePath, expPath + EXP_EXT);
      final String springXml;
      try {
        springXml = CharStreams.toString(Files.newReaderSupplier(sourceFile, Charsets.UTF_8));
      } catch (IOException e) {
        throw Throwables.propagate(e);
      }
      final ImportDomain.Experiment meta = readMeta(springXml);
      if (meta == null) {
        log.warn("no meta in experiment config: {}", sourceFile.getPath());
        continue;
      }

      propagateDefault(meta);

      final Experiment exp;
      try {
        final boolean expUpdated = dao.insertExperiment(
            meta.getId(), expPath, meta.getDepends(), meta.getDesc(), springXml, sourceFile.lastModified()
        );

        if (!expUpdated) {
          log.debug("NOT updated experiment with path '{}'", expPath);
          continue;
        }

        log.info("updated experiment with path '{}'", expPath);
        exp = dao.findExperimentByPath(expPath);
      } catch (SQLException e) {
        throw Throwables.propagate(e);
      }

      for (ImportDomain.Config config : meta.getConfigs().values()) {
        try {
          final long confUid = dao.insertConf(
              exp.getUid(), config.getName(), config.getDesc()
          );

          for (ImportDomain.ParamSpec paramSpec : config.getParamSpecs().values()) {
            dao.insertParam(
                confUid, paramSpec.getName(), paramSpec.getValueSpec(), paramSpec.isInternal(), paramSpec.getDesc()
            );
          }
        } catch (SQLException e) {
          throw Throwables.propagate(e);
        }
      }
    }
  }

  protected void propagateDefault(ImportDomain.Experiment meta) {
    final ImportDomain.Config defaultConf = meta.getConfigs().get("default");
    if (defaultConf == null) {
      return;
    }

    final SortedMap<String, ImportDomain.ParamSpec> temp = new TreeMap<String, ImportDomain.ParamSpec>();
    for (ImportDomain.Config config : meta.getConfigs().values()) {
      if ("default".equalsIgnoreCase(config.getName())) {
        continue;
      }

      temp.clear();
      //  grab all default parameters
      temp.putAll(defaultConf.getParamSpecs());
      //  remove those which are specified by current config itself
      temp.keySet().removeAll(config.getParamSpecs().keySet());
      //  and then add to current config
      config.getParamSpecs().putAll(temp);
    }
  }

  protected static ImportDomain.Experiment readMeta(String springXml) {
    final ImportDomain.Experiment[] experimentArr = new ImportDomain.Experiment[1];
    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setNamespaceAware(true);
      SAXParser saxParser = factory.newSAXParser();

      DefaultHandler handler = new DefaultHandler() {
        private ImportDomain.Config conf = null;

        @Override
        public void startElement(
            String uri, String localName, String qName, Attributes attributes
        ) throws SAXException {
          if ("Experiment".equalsIgnoreCase(localName)) {
            final ImportDomain.Experiment exp = new ImportDomain.Experiment();

            exp.setId(attributes.getValue("id"));
            exp.setDesc(attributes.getValue("description"));
            exp.setDepends(attributes.getValue("depends"));

            experimentArr[0] = exp;
          }

          if ("Config".equalsIgnoreCase(localName)) {
            conf = new ImportDomain.Config();

            final String name = attributes.getValue("name");
            conf.setName(name);
            conf.setDesc(attributes.getValue("description"));

            experimentArr[0].getConfigs().put(name, conf);
          }

          if ("Param".equalsIgnoreCase(localName)) {
            final ImportDomain.ParamSpec paramSpec = new ImportDomain.ParamSpec();

            final String name = attributes.getValue("name");
            paramSpec.setName(name);
            final String desc = attributes.getValue("description");
            paramSpec.setDesc(desc == null ? "" : desc);  //  this param is not required, while DB yells at nulls...
            paramSpec.setInternal(Boolean.valueOf(attributes.getValue("internal")));
            paramSpec.setValueSpec(attributes.getValue("value"));

            conf.getParamSpecs().put(name, paramSpec);
          }
        }
      };

      saxParser.parse(new ByteArrayInputStream(springXml.getBytes(Charset.forName("UTF-8"))), handler);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return experimentArr[0];
  }

  protected String[] listExperimentPaths() {
    final File base = new File(basePath);
    final File[] expFiles = base.listFiles(new ExpFileFilter());

    final String[] strings = new String[expFiles.length];

    for (int i = 0; i < expFiles.length; i++) {
      final String name = expFiles[i].getName();
      strings[i] = name.substring(0, name.length() - EXP_EXT.length());
    }

    return strings;
  }
}

class ExpFileFilter implements FileFilter {
  public boolean accept(File file) {
    final boolean nonEmptyFile = file.isFile() && file.length() > 0;
    final boolean accept = nonEmptyFile && file.getName().endsWith(ImportRunnable.EXP_EXT);

    return accept;
  }
}
