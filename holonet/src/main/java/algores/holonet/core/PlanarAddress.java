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

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * The implementation of NodeAddress for the network. Use an address from 2-d euclidean space.
 */
class PlanarAddress implements Address {
  public static final double SCALE = 1000.0;

  protected final double coordX;
  protected final double coordY;
  protected final Key key;
  protected final int hashCode;

  PlanarAddress(double coordY, double coordX) {
    this.coordY = coordY;
    this.coordX = coordX;

    this.key = API.createKey(this); //  TODO we have hash collisions now

    int hashCode;
    long temp = coordX != +0.0d ? Double.doubleToLongBits(coordX) : 0L;
    hashCode = (int) (temp ^ (temp >>> 32));
    temp = coordY != +0.0d ? Double.doubleToLongBits(coordY) : 0L;
    hashCode = 31 * hashCode + (int) (temp ^ (temp >>> 32));
    this.hashCode = hashCode;
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

  /**
   * Please note that getKey() should return the same value for given instance of address,
   * as it is used in hashCode() and equals().
   */
  public Key getKey() {
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

    final PlanarAddress that = (PlanarAddress) o;

    return
        this.coordX == that.coordX &&
        this.coordY == that.coordY;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  public int compareTo(algores.holonet.capi.Address thatAddr) {
    final PlanarAddress that = (PlanarAddress) thatAddr;
    int xRes = Double.compare(this.coordX, that.coordX);
    return xRes != 0 ? xRes : Double.compare(this.coordY, that.coordY);
  }

  public Address getAddress() {
    return this;
  }
}
