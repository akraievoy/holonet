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

public class RunBean implements Run {
  protected final long uid;
  protected final long expUid;
  protected final long confUid;
  protected final long millis;
  protected final long psetCount;
  protected final long psetComplete;

  protected String expDesc;
  protected String confDesc;
  protected String chain = "";  //  TODO morph to long[]

  public RunBean(long uid, long expUid, long confUid, long millis, long psetCount, long psetComplete) {
    this.confUid = confUid;
    this.expUid = expUid;
    this.uid = uid;
    this.millis = millis;
    this.psetCount = psetCount;
    this.psetComplete = psetComplete;
  }

  public boolean isComplete() {
    return getPsetCount() == getPsetComplete();
  }

  public long getConfUid() {
    return confUid;
  }

  public long getExpUid() {
    return expUid;
  }

  public long getUid() {
    return uid;
  }

  public long getMillis() {
    return millis;
  }

  public long getPsetCount() {
    return psetCount;
  }

  public long getPsetComplete() {
    return psetComplete;
  }

  public String getConfDesc() {
    return confDesc;
  }

  public void setConfDesc(String confDesc) {
    this.confDesc = confDesc;
  }

  public String getExpDesc() {
    return expDesc;
  }

  public String getChain() {
    return chain;
  }

  public void setChain(String chain) {
    this.chain = chain;
  }

  public void setExpDesc(String expDesc) {
    this.expDesc = expDesc;
  }

  @Override
  public String toString() {
    return "[" + uid + ":" + expUid + "@" + confUid + "] ('" + expDesc + "'@'" + confDesc + "')";
  }
}
