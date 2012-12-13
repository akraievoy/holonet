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

public class ConfBean implements Conf {
  private long uid;
  private long expUid;
  private String name;
  private String desc;

  public ConfBean() {
  }

  public ConfBean(
      long uid, long expUid,
      String name, String desc
  ) {
    this.expUid = expUid;
    this.uid = uid;
    this.desc = desc;
    this.name = name;
  }

  public long getExpUid() {
    return expUid;
  }

  public long getUid() {
    return uid;
  }

  public String getDesc() {
    return desc;
  }

  public String getName() {
    return name;
  }

  public void setUid(long uid) {
    this.uid = uid;
  }

  public void setExpUid(long expUid) {
    this.expUid = expUid;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }
}
