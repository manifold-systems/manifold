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

import manifold.science.api.AbstractPrimaryUnit;
import manifold.science.api.UnitCache;
import manifold.science.util.Rational;


import static manifold.science.util.CoercionConstants.r;

public final class ChargeUnit extends AbstractPrimaryUnit<Charge, ChargeUnit>
{
  private static final UnitCache<ChargeUnit> CACHE = new UnitCache<>();
  public static ChargeUnit get( Rational coulombs, String name, String symbol )
  {
    return CACHE.get( new ChargeUnit( coulombs, name, symbol ) );
  }

  public static final ChargeUnit Coulomb = get( 1r, "Coulomb", "C" );
  public static final ChargeUnit Elementary = get( 1.6021766208e-19r, "Elementary", "e" );

  public static final ChargeUnit BASE = Coulomb;

  public ChargeUnit( Rational coulombs, String name, String symbol )
  {
    super( coulombs, name, symbol );
  }

  public Rational getCoulombs()
  {
    return toNumber();
  }

  @Override
  public Charge makeDimension( Number amount )
  {
    return new Charge( Rational.get( amount ), this );
  }

  public CurrentUnit div( TimeUnit time )
  {
    return CurrentUnit.get( this, time );
  }

  public TimeUnit div( CurrentUnit i )
  {
    return i.getTimeUnit();
  }

  public CapacitanceUnit div( PotentialUnit p )
  {
    return CapacitanceUnit.get( this, p );
  }

  public PotentialUnit div( CapacitanceUnit cu )
  {
    return cu.getPotentialUnit();
  }
}
