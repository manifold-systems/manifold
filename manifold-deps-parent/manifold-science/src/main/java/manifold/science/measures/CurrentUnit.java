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


import static manifold.science.measures.ChargeUnit.Coulomb;
import static manifold.science.util.CoercionConstants.r;
import static manifold.science.measures.TimeUnit.Second;

public final class CurrentUnit extends AbstractQuotientUnit<ChargeUnit, TimeUnit, Current, CurrentUnit>
{
  private static final UnitCache<CurrentUnit> CACHE = new UnitCache<>();

  public static final CurrentUnit A = get( Coulomb, Second, 1 r, "Amperes", "A" );

  public static final CurrentUnit BASE = A;

  public static CurrentUnit get( ChargeUnit chargeUnit, TimeUnit timeUnit )
  {
    return get( chargeUnit, timeUnit, null, null, null );
  }

  public static CurrentUnit get( ChargeUnit chargeUnit, TimeUnit timeUnit, Rational factor, String name, String symbol )
  {
    CurrentUnit unit = new CurrentUnit( chargeUnit, timeUnit, factor, name, symbol );
    return CACHE.get( unit );
  }

  private CurrentUnit( ChargeUnit chargeUnit, TimeUnit timeUnit, Rational factor, String name, String symbol )
  {
    super( chargeUnit, timeUnit, factor, name, symbol );
  }

  @Override
  public Current makeDimension( Number amount )
  {
    return new Current( Rational.get( amount ), this );
  }

  public ChargeUnit getChargeUnit()
  {
    return getLeftUnit();
  }

  public TimeUnit getTimeUnit()
  {
    return getRightUnit();
  }

  public ConductanceUnit div( PotentialUnit p )
  {
    return ConductanceUnit.get( this, p );
  }

  public PotentialUnit div( ConductanceUnit cu )
  {
    return cu.getPotentialUnit();
  }
}
