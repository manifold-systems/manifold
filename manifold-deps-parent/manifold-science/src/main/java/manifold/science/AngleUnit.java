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

import manifold.science.api.AbstractPrimaryUnit;
import manifold.science.api.UnitCache;
import manifold.science.util.Rational;


import static manifold.science.MetricScaleUnit.*;
import static manifold.science.util.DimensionlessConstants.*;
import static manifold.science.util.CoercionConstants.r;

public final class AngleUnit extends AbstractPrimaryUnit<Angle, AngleUnit>
{
  private static final UnitCache<AngleUnit> CACHE = new UnitCache<>();
  public static AngleUnit get( Rational radians, String name, String symbol )
  {
    return CACHE.get( new AngleUnit( radians, name, symbol ) );
  }

  public static final AngleUnit Nano = get( 1n, "Nanoradian", "nrad" );
  public static final AngleUnit Milli = get( 1m, "Milliradian", "mrad" );
  public static final AngleUnit Radian = get( 1r, "Radian", "rad" );
  public static final AngleUnit Degree = get( pi / 180, "Degree", "deg" );
  public static final AngleUnit MOA = get( pi / 10800, "MinuteOfArc", "moa" );
  public static final AngleUnit ArcSecond = get( pi / 648k, "ArcSecond", "arcsec" );
  public static final AngleUnit MilliArcSecond = get( pi / 648M, "MilliArcSecond", "mas" );
  public static final AngleUnit Turn = get( 2 * pi, "Turn", "cyc" );
  public static final AngleUnit Gradian = get( pi / 200, "Gradian", "grad" );
  public static final AngleUnit Quadrant = get( pi / 2, "Quadrant", "quad" );

  public static final AngleUnit BASE = Radian;

  private AngleUnit( Rational radians, String name, String symbol )
  {
    super( radians, name, symbol );
  }

  @Override
  public Angle makeDimension( Number amount )
  {
    return new Angle( Rational.get( amount ), this );
  }

  public FrequencyUnit div( TimeUnit time )
  {
    return FrequencyUnit.get( this, time );
  }

  public TimeUnit div( FrequencyUnit freq )
  {
    return freq.getTimeUnit();
  }
}
