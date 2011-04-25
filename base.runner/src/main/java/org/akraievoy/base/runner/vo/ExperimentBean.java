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

package org.akraievoy.base.runner.vo;

public class ExperimentBean implements Experiment {
  private final long uid;
  private final String id;
  private final long millis;
  private final String path;
  private final String depends;
  private final String desc;
  private final String springXml;

  public ExperimentBean(
      long uid, String id, String path,
      String depends, String desc, long millis,
      String springXml
  ) {
    this.uid = uid;
    this.id = id;
    this.path = path;
    this.depends = depends;
    this.desc = desc;
    this.millis = millis;
    this.springXml = springXml;
  }

  public String getDesc() {
    return desc;
  }

  public String getPath() {
    return path;
  }

  public long getUid() {
    return uid;
  }

  public long getMillis() {
    return millis;
  }

  public String getSpringXml() {
    return springXml;
  }

  public String getDepends() {
    return depends;
  }

  public String getId() {
    return id;
  }
}
