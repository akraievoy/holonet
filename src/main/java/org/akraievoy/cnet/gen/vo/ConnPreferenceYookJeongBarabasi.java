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

package org.akraievoy.cnet.gen.vo;

public class ConnPreferenceYookJeongBarabasi implements ConnPreference {
  protected double alpha = 1;
  protected double beta = 1;

  /**
   * Controls degree preference, recommended value is 1.
   */
  public void setAlpha(double alpha) {
    this.alpha = alpha;
  }

  /**
   * Controls distance preference, recommended value is 1.
   * <br/>
   * <b>Note</b>: that'a actually a sigma in YJB paper.
   */
  public void setBeta(double beta) {
    this.beta = beta;
  }

/*
  int sampler = 0;
*/

  public double getPreference(double degree, double dist) {
    if (dist < 0) {
      throw new IllegalArgumentException("dist < 0");
    }
    if (dist == 0) {
      return Math.pow(degree, alpha);
    }
    final double pref = Math.pow(degree, alpha) / Math.pow(dist, beta);
/*
    if (sampler == 0) {
      System.out.println("dist = " + dist + " => " + pref);
    }
    sampler = (sampler + 1) % 16384;
*/
    return pref;
  }
}