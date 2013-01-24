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

package org.akraievoy.cnet.metrics.api;

import org.akraievoy.base.ref.Ref;
import org.akraievoy.base.ref.RefRO;
import org.akraievoy.holonet.exp.store.RefObject;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.Routes;

public abstract class MetricRoutes extends Metric<Routes> {
  public Ref<Routes> target = new RefObject<Routes>();

  @SuppressWarnings("unchecked")
  public void setTarget(Ref<? extends Routes> target) {
    this.target = (Ref<Routes>) target;
  }

  public Ref<? extends Routes> getTarget() {
    return target;
  }

  public abstract void setDistSource(RefRO<? extends EdgeData> distSource);

  public abstract void setSource(RefRO<? extends EdgeData> source);
}
