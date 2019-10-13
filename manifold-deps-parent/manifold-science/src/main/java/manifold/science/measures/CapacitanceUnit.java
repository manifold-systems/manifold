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

public final class CapacitanceUnit extends AbstractQuotientUnit<ChargeUnit, PotentialUnit, Capacitance, CapacitanceUnit>
{
  private static final UnitCache<CapacitanceUnit> CACHE = new UnitCache<>();

  public static final CapacitanceUnit F = get( ChargeUnit.BASE, PotentialUnit.BASE, 1 r, "Farad", "F" );

  public static final CapacitanceUnit BASE = F;

  public static CapacitanceUnit get( ChargeUnit chargeUnit, PotentialUnit potentialUnit )
  {
    return get( chargeUnit, potentialUnit, null, null, null );
  }

  public static CapacitanceUnit get( ChargeUnit chargeUnit, PotentialUnit potentialUnit, Rational factor, String name, String symbol )
  {
    CapacitanceUnit unit = new CapacitanceUnit( chargeUnit, potentialUnit, factor, name, symbol );
    return CACHE.get( unit );
  }

  private CapacitanceUnit( ChargeUnit chargeUnit, PotentialUnit potentialUnit, Rational factor, String name, String symbol )
  {
    super( chargeUnit, potentialUnit, factor, name, symbol );
  }

  @Override
  public Capacitance makeDimension( Number amount )
  {
    return new Capacitance( Rational.get( amount ), this );
  }

  public ChargeUnit getChargeUnit()
  {
    return getLeftUnit();
  }

  public PotentialUnit getPotentialUnit()
  {
    return getRightUnit();
  }
}
