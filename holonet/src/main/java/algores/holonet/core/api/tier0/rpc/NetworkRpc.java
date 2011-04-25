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

/**
 * Created by IntelliJ IDEA.
 * User: ak
 * Date: Nov 10, 2008
 * Time: 10:44:29 PM
 * To change this template use File | Settings | File Templates.
 */
public interface NetworkRpc {
  <E> E getProxy(Node node, Address calleeAddress, Class<E> service);

  Call getCall();
}
