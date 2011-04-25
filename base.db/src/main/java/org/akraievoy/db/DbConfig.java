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

package org.akraievoy.db;

public class DbConfig {
  protected String url;
  protected String driverClass = "org.h2.Driver";
  protected String login = "sa";
  protected String password = "";
  protected boolean sync = true;
  protected String validator = "SELECT * FROM INFORMATION_SCHEMA.INDEXES";
  protected int connectionMaxNum = 1;
  protected int connectionTimeout = 20;

  /**
   * @return JDBC driver class name
   */
  public String getDriverClass() {
    return driverClass;
  }

  public void setDriverClass(String driverClass) {
    this.driverClass = driverClass;
  }

  /**
   * @return db connection url
   */
  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * @return db connection login
   */
  public String getLogin() {
    return login;
  }

  public void setLogin(String login) {
    this.login = login;
  }

  /**
   * @return db connection password
   */
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * @return statement used to validate a pooled connection
   */
  public String getValidator() {
    return validator;
  }

  public void setValidator(String validator) {
    this.validator = validator;
  }

  /**
   * Synchronize transactions over Db instance?
   * H2 seems to ged freaked out when multiple threads beat it at once, so defaults to true.
   */
  public boolean isSync() {
    return sync;
  }

  public void setSync(boolean sync) {
    this.sync = sync;
  }

  public int getConnectionMaxNum() {
    return connectionMaxNum;
  }

  public void setConnectionMaxNum(int connectionMaxNum) {
    this.connectionMaxNum = connectionMaxNum;
  }

  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(int connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }
}
