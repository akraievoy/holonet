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

package org.akraievoy.holonet.runner.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleTest implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(SimpleTest.class);

  public SimpleTest() {
  }

  public void run() {
    try {
      log.info("that's an info, sleeping for {} secs", 10);
      Thread.sleep(5 * 1000);
      log.debug("that's a debug, {} secs more left...", 5);
      Thread.sleep(5 * 1000);
      log.debug("that's a debug");
      Thread.sleep(1000);
      log.warn("that's a warn");
      Thread.sleep(1000);
      log.debug("that's another debug");
      Thread.sleep(2 * 1000);
      log.trace("a trace!!!");
      Thread.sleep(2 * 1000);
      log.error("that's an error");
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      //	ignored
    }
  }
}
