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

import manifold.science.api.AbstractQuotientUnit;
import manifold.science.api.UnitCache;
import manifold.science.util.Rational;


import static manifold.science.LengthUnit.Meter;
import static manifold.science.LengthUnit.Mile;
import static manifold.science.TimeUnit.Hour;
import static manifold.science.TimeUnit.Second;

final public class VelocityUnit extends AbstractQuotientUnit<LengthUnit, TimeUnit, Velocity, VelocityUnit>
{
  private static final UnitCache<VelocityUnit> CACHE = new UnitCache<>();

  public static final VelocityUnit BASE = get( Meter, Second );
  public static final VelocityUnit mph = get( Mile, Hour, Rational.ONE, "MPH", "mph" );

  public static VelocityUnit get( LengthUnit lengthUnit, TimeUnit timeUnit )
  {
    return get( lengthUnit, timeUnit, null, null, null );
  }
  public static VelocityUnit get( LengthUnit lengthUnit, TimeUnit timeUnit, Rational factor, String name, String symbol )
  {
    VelocityUnit unit = new VelocityUnit( lengthUnit, timeUnit, factor, name, symbol );
    return CACHE.get( unit );
  }

  private VelocityUnit( LengthUnit lengthUnit, TimeUnit timeUnit, Rational factor, String name, String symbol )
  {
    super( lengthUnit, timeUnit, factor, name, symbol );
  }

  @Override
  public Velocity makeDimension( Number amount )
  {
    return new Velocity( Rational.get( amount ), this );
  }

  public LengthUnit getLengthUnit()
  {
    return getLeftUnit();
  }

  public TimeUnit getTimeUnit()
  {
    return getRightUnit();
  }

  public MomentumUnit postfixBind( MassUnit mass ) {
    return times( mass );
  }

  public AccelerationUnit div( TimeUnit t ) {
    return AccelerationUnit.get( this, t );
  }

  public MomentumUnit times( MassUnit t ) {
    return MomentumUnit.get( t, this );
  }

  public PowerUnit times( ForceUnit force ) {
    return force * getLengthUnit() / getTimeUnit();
  }
}
