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

package algores.holonet.protocols.pgrid;

import algores.holonet.core.CommunicationException;
import algores.holonet.core.api.Key;
import algores.holonet.core.api.Range;
import algores.holonet.core.api.tier1.overlay.OverlayService;

import java.util.Set;

/**
 * TOAK general overview javadoc.
 */

public interface PGrid extends OverlayService {
  PGridImpl.InviteResponse invite(Range rPath, Set<Key> pKeys) throws CommunicationException;

  /**
   * Reverse request part of a split operation.
   */
  PGridImpl.SplitData splitCallback(Range newLocalPath, PGridImpl.SplitData sData, Range newRemotePath, boolean remove, String operation) throws CommunicationException;
}
