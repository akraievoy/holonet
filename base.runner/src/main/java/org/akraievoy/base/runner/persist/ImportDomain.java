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

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Classes reflecting the XML experiment configuration model.
 * <p/>
 * I don't want to mess with preexisting domain classes as there're already some differences.
 */
public class ImportDomain {
  public static class Experiment {
    private String id;
    private String depends;
    private String desc;
    private final SortedMap<String, Config> configs = new TreeMap<String, Config>();

    public String getDepends() { return depends; }
    public void setDepends(String depends) { this.depends = depends; }

    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public SortedMap<String, Config> getConfigs() { return configs; }
  }

  public static class Config {
    private String name;
    private String desc;
    private final SortedMap<String, ParamSpec> paramSpecs = new TreeMap<String, ParamSpec>();

    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public SortedMap<String, ParamSpec> getParamSpecs() { return paramSpecs; }
  }

  public static class ParamSpec {
    private String name;
    private String desc;
    private String valueSpec;
    private boolean internal = false;

    public boolean isInternal() { return internal; }
    public void setInternal(boolean internal) { this.internal = internal; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }

    public String getValueSpec() { return valueSpec; }
    public void setValueSpec(String valueSpec) { this.valueSpec = valueSpec; }
  }
}
