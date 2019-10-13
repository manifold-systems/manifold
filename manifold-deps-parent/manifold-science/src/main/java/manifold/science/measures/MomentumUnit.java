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

import manifold.science.api.AbstractProductUnit;
import manifold.science.api.UnitCache;
import manifold.science.util.Rational;


import static manifold.science.measures.MassUnit.Kilogram;

public final class MomentumUnit extends AbstractProductUnit<MassUnit, VelocityUnit, Momentum, MomentumUnit>
{
  private static final UnitCache<MomentumUnit> CACHE = new UnitCache<>();

  public static final MomentumUnit BASE = get( Kilogram, VelocityUnit.BASE );

  public static MomentumUnit get( MassUnit massUnit, VelocityUnit velocityUnit )
  {
    return get( massUnit, velocityUnit, null, null, null );
  }

  public static MomentumUnit get( MassUnit massUnit, VelocityUnit velocityUnit, Rational factor, String name, String symbol )
  {
    MomentumUnit unit = new MomentumUnit( massUnit, velocityUnit, factor, name, symbol );
    return CACHE.get( unit );
  }

  private MomentumUnit( MassUnit massUnit, VelocityUnit velocityUnit, Rational factor, String name, String symbol )
  {
    super( massUnit, velocityUnit, factor, name, symbol );
  }

  @Override
  public Momentum makeDimension( Number amount )
  {
    return new Momentum( Rational.get( amount ), this );
  }

  public MassUnit getMassUnit()
  {
    return getLeftUnit();
  }

  public VelocityUnit getVelocityUnit()
  {
    return getRightUnit();
  }

  public EnergyUnit times( VelocityUnit v )
  {
    return EnergyUnit.get( getMassUnit() * (getVelocityUnit() / v.getTimeUnit()), v.getLengthUnit() );
  }

  public MassUnit div( VelocityUnit w )
  {
    return getMassUnit();
  }
}
