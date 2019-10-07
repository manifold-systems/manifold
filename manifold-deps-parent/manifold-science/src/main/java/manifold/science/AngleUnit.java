/*
 * Copyright (c) 2019 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.science;

import manifold.science.api.Unit;
import manifold.science.util.Rational;


import static manifold.science.MetricScaleUnit.*;
import static manifold.science.util.DimensionlessConstants.*;

public enum AngleUnit implements Unit<Angle, AngleUnit>
{
  Nano( 1n, "Nanoradian", "nrad" ),
  Milli( 1m, "Milliradian", "mrad" ),
  Radian( 1r, "Radian", "rad" ),
  Degree( pi/180, "Degree", "deg" ),
  MOA( pi/10800, "MinuteOfArc", "moa" ),
  ArcSecond( pi/648k, "ArcSecond", "arcsec" ),
  MilliArcSecond( pi/648M, "MilliArcSecond", "mas" ),
  Turn( 2*pi, "Turn", "cyc" ),
  Gradian( pi/200, "Gradian", "grad" ),
  Quadrant( pi/2, "Quadrant", "quad" );

  private Rational _rads;
  private String _name;
  private String _symbol;

  public static AngleUnit BASE = Radian;

  AngleUnit( Rational rads, String name, String symbol ) {
    _rads = rads;
    _name = name;
    _symbol = symbol;
  }

  @Override
  public Angle makeDimension( Number amount )
  {
    return new Angle( Rational.get( amount ), this );
  }

  public Rational getRads()
  {
    return _rads;
  }

  public String getUnitName() {
    return _name;
  }

   public String getUnitSymbol() {
    return _symbol;
  }

  public Rational toBaseUnits( Rational myUnits ) {
    return _rads * myUnits;
  }

  public Rational toNumber() {
    return _rads;
  }

  public Rational from( Angle angle ) {
    return angle.toBaseNumber() / _rads;
  }

  public FrequencyUnit div( TimeUnit time ) {
    return FrequencyUnit.get( this, time );
  }

  public TimeUnit div( FrequencyUnit freq ) {
    return freq.getTimeUnit();
  }
}
