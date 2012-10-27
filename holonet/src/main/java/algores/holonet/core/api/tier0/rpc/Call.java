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

package algores.holonet.core.api.tier0.rpc;

import algores.holonet.core.Node;
import algores.holonet.core.api.Address;

public class Call {
  final Node source;
  final Address target;
  final Class service;

  public Call(Node rpcSource, Address rpcTarget, Class service) {
    this.source = rpcSource;
    this.target = rpcTarget;
    this.service = service;
  }

  public Node getSource() {
    return source;
  }

  public Address getTarget() {
    return target;
  }

  public Class getService() {
    return service;
  }

  @Override
  public String toString() {
    return String.format(
        "%s ==> %s.%s",
        source,
        target,
        service.getSimpleName()
    );
  }
}
