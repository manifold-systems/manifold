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

package manifold.science.measures;

import manifold.science.api.AbstractQuotientUnit;
import manifold.science.api.UnitCache;
import manifold.science.util.Rational;


import static manifold.science.util.CoercionConstants.r;

public final class AccelerationUnit extends AbstractQuotientUnit<VelocityUnit, TimeUnit, Acceleration, AccelerationUnit>
{
  private static final UnitCache<AccelerationUnit> CACHE = new UnitCache<>();

  public static final AccelerationUnit BASE = get( VelocityUnit.BASE, VelocityUnit.BASE.getTimeUnit() );
  public static final AccelerationUnit GRAVITY = get( VelocityUnit.BASE, VelocityUnit.BASE.getTimeUnit(), 9.80665r, "Gravity", "g" );

  public static AccelerationUnit get( VelocityUnit velocityUnit, TimeUnit timeUnit )
  {
    return get( velocityUnit, timeUnit, null, null, null );
  }

  public static AccelerationUnit get( VelocityUnit velocityUnit, TimeUnit timeUnit, Rational factor, String name, String symbol )
  {
    AccelerationUnit unit = new AccelerationUnit( velocityUnit, timeUnit, factor, name, symbol );
    return CACHE.get( unit );
  }

  private AccelerationUnit( VelocityUnit velocityUnit, TimeUnit timeUnit, Rational factor, String name, String symbol )
  {
    super( velocityUnit, timeUnit, factor, name, symbol );
  }

  @Override
  public Acceleration makeDimension( Number amount )
  {
    return new Acceleration( Rational.get( amount ), this );
  }

  public String getFullName()
  {
    return getVelocityUnit().getTimeUnit() == getTimeUnit()
           ? getVelocityUnit().getLengthUnit().getFullName() + "/" + getTimeUnit().getFullName() + "\u00B2"
           : getVelocityUnit().getFullName() + "/" + getTimeUnit().getFullName();
  }

  public String getFullSymbol()
  {
    return getVelocityUnit().getTimeUnit() == getTimeUnit()
           ? getVelocityUnit().getLengthUnit().getFullSymbol() + "/" + getTimeUnit().getFullSymbol() + "\u00B2"
           : getVelocityUnit().getFullSymbol() + "/" + getTimeUnit().getFullSymbol();
  }

  public VelocityUnit getVelocityUnit()
  {
    return getLeftUnit();
  }

  public TimeUnit getTimeUnit()
  {
    return getRightUnit();
  }

  public ForceUnit postfixBind( MassUnit mass )
  {
    return times( mass );
  }

  public ForceUnit times( MassUnit t )
  {
    return ForceUnit.get( t, this );
  }
}
