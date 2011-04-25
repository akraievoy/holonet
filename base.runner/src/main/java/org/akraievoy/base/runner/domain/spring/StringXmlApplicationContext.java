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

package org.akraievoy.base.runner.domain.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.Resource;

public class StringXmlApplicationContext extends AbstractXmlApplicationContext {
  protected Resource[] resources;

  public StringXmlApplicationContext(final String xmlSource, final boolean refresh, ApplicationContext parent) {
    super(parent);
    resources = new Resource[]{new StringResource(xmlSource)};
    if (refresh) {
      refresh();
    }
  }

  protected Resource[] getConfigResources() {
    return resources;
  }
}

