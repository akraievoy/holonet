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

package algores.holonet.capi;

/**
 * Actual routing facility would like to receive much softer events than simple join/leave updates
 * which original authors propose.
 */
public enum Event {
  /**
   * New node joined the network
   */
  JOINED,
  /**
   * Graceful leave
   */
  LEFT,
  /**
   * Connection failure
   */
  CONNECTION_FAILED,
  /**
   * Discovered while stabilizing, indirect discovery via routing table data exchange
   */
  DISCOVERED,
  /**
   * Stabilization update/exchange of routing tables
   */
  HEART_BEAT
}
