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

package org.akraievoy.base.runner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {
  public static void main(String[] args) {
    BasicConfigurator.configure();
    Logger.getLogger("org.springframework").setLevel(Level.WARN);
    Logger.getLogger("org.akraievoy").setLevel(Level.TRACE);
    Logger.getLogger("org.akraievoy.base.runner.domain.spring").setLevel(Level.WARN);

    final ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
        new String[]{"/org/akraievoy/base/runner/runner-beans.xml"},
        false
    );

    context.registerShutdownHook();

    context.refresh();
  }
}
