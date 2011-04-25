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

package algores.holonet.core;

/**
 * If node failed, we should handle this.
 */
public class CommunicationException extends SimulatorException {
  public CommunicationException(Throwable cause) {
    super(cause);
  }

  public CommunicationException(String message) {
    super(message);
  }

  public CommunicationException(String message, Throwable cause) {
    super(message, cause);
  }
}
