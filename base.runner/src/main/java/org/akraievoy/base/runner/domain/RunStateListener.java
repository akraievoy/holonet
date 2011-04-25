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

package org.akraievoy.base.runner.domain;

public interface RunStateListener {
  void onPsetAdvance(long runId, long index);

  void onRunCreation(long runId);

  RunStateListener NOOP = new RunStateListener() {
    public void onPsetAdvance(long runId, long index) {
      //  nothing to do
    }

    public void onRunCreation(long runId) {
      //  nothing to do
    }
  };
}
