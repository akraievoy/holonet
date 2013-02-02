package org.akraievoy.util;

import java.lang.Math;

public class Interpolate {
  public static interface Fun {
    public double apply(double at);
  }

  /**
   * Interpolates from 0 to 1 on interval from 0 to 1.
   * http://en.wikipedia.org/wiki/Logistic_function
   */
  public static final Fun LOGISTIC = new Fun() {
    @Override
    public double apply(double at) {
      return 1 / (1 + Math.exp(-(at*12 - 6)));
    }
  };

  /*
   * Interpolates from 0 to 1 as x**pow.
   * Has more delta at 1 for pows > 1 and more delta at 0 for 0 < pows < 1
   */
  public static Fun pow(final double pow) {
    if (0 >= pow) {
      throw new IllegalArgumentException("pow " + pow + " <= 0");
    }

    return new Fun() {
      @Override
      public double apply(double at) {
        if (at <= 0) {
          return 0;
        }

        if (at >= 1) {
          return 1;
        }

        if (pow == 1) {
          return at;
        }

        return Math.pow(at, pow);
      }
    };
  }

  /** Interpolates linearly from 0 to 1 */
  public static final Fun LINEAR = pow(1);
  /** Interpolates from 0 to 1 with greater delta at 1 */
  public static final Fun POW2 = pow(2);
  /** Interpolates from 0 to 1 with greater delta at 0 */
  public static final Fun SQRT = pow(0.5);

  /**
   * Interpolates from 0 to 1 on interval from 0 to 1.
   * Uses base 2 log from 1 to 16, with most of delta at 0.
   */
  public static final Fun LOG = new Fun() {
    @Override
    public double apply(double at) {
      if (at <= 0) {
        return 0;
      }

      if (at >= 1) {
        return 1;
      }

      return Math.log(1 + at * 15) / Math.log(16) / 4;
    }
  };

  public static Fun norm(
      final double fromAt,
      final double intoAt,
      final double fromVal,
      final double intoVal,
      final Fun fun
  ) {
    return new Fun() {
      @Override
      public double apply(double at) {
        double atNorm = (at - fromAt) / (intoAt - fromAt);
        double resNorm = fun.apply(atNorm);
        double res = fromVal + resNorm * (intoVal - fromVal);
        return res;
      }
    };
  }

}
