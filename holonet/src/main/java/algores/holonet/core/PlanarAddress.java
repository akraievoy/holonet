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

import algores.holonet.core.api.API;
import algores.holonet.core.api.Address;
import algores.holonet.core.api.Key;
import org.akraievoy.cnet.gen.vo.EntropySource;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * The implementation of NodeAddress for the network. Use an address from 2-d euclidean space.
 */
class PlanarAddress implements Address {
  public static final double SCALE = 1000.0;
  public static final double EQUALITY_THRESH = 1.0e-12;

  protected double coordX;
  protected double coordY;
  protected Key key;

  PlanarAddress() {
  }

  public double getDistance(Address address) {
    if (!(address instanceof PlanarAddress)) {
      throw new IllegalArgumentException("Should use only 2d addresses?");
    }

    PlanarAddress planarAddress = (PlanarAddress) address;

    final double xDiff = coordX - planarAddress.coordX;
    final double yDiff = coordY - planarAddress.coordY;
    final double distSqr = Math.pow(xDiff, 2.0) + Math.pow(yDiff, 2.0);

    return Math.sqrt(distSqr) * SCALE;
  }

  public void init(EntropySource eSource) {
    coordX = eSource.nextDouble();
    coordY = eSource.nextDouble();
  }

  /**
   * Please note that getKey() should return the same value for given instance of address,
   * as it is used in hashCode() and equals().
   */
  public Key getKey() {
    if (key == null) {
      key = API.createKey(this); //  TODO we have hash collisions now
    }
    return key;
  }

  protected static final NumberFormat nf = DecimalFormat.getNumberInstance();

  static {
    nf.setMaximumFractionDigits(3);
    nf.setMinimumFractionDigits(3);
  }

  public String toString() {
    return "(" + nf.format(coordX) + ":" + nf.format(coordY) + ")";
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PlanarAddress)) return false;

    final PlanarAddress planarAddress = (PlanarAddress) o;

    return getKey().equals(planarAddress.getKey());
  }

  public int hashCode() {
    return getKey().hashCode();
  }

  public int compareTo(algores.holonet.capi.Address anObject) {
    final PlanarAddress anotherAddress = (PlanarAddress) anObject;
    return getKey().compareTo(anotherAddress.getKey());
  }

  public Address getAddress() {
    return this;
  }
}
