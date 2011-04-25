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

import algores.common.WrappedException;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;

/**
 * Package entries.
 *
 * @author Anton Kraievoy
 */
public class API {
  private API() {
    // sealed
  }

  public static Key createKey(Object obj) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("MD5");
      messageDigest.update(obj.toString().getBytes());
      return new KeyBase(messageDigest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new WrappedException(e);
    }
  }

  public static Key createKey(byte[] digestData) {
    return new KeyBase(digestData);
  }

  public static Key createKey(BigInteger number) {
    return new KeyBase(number);
  }

  protected static Key createBaseKey(BitSet newKeyData) {
    return new KeyBase(newKeyData);
  }
}
