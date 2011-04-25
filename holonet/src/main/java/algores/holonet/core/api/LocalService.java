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

package algores.holonet.core.api;

import algores.holonet.core.Node;

/**
 * Common operation for local view of a service.
 */
public interface LocalService {
  void init(Node ownerNode);

  Node getOwner();

  /**
   * This is called before init(), and allows service factory to populate each node's services.
   * Using a default constructor suffices as long as your subclass does not have any configurable properties.
   *
   * @return a copy of current service object, with replicated property setup (if any).
   */
  LocalServiceBase copy();
}
