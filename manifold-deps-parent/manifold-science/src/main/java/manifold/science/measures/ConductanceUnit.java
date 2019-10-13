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

public final class ConductanceUnit extends AbstractQuotientUnit<CurrentUnit, PotentialUnit, Conductance, ConductanceUnit>
{
  private static final UnitCache<ConductanceUnit> CACHE = new UnitCache<>();

  public static final ConductanceUnit S = get( CurrentUnit.BASE, PotentialUnit.BASE, 1 r, "Siemens", "S" );

  public static final ConductanceUnit BASE = S;

  public static ConductanceUnit get( CurrentUnit currentUnit, PotentialUnit potentialUnit )
  {
    return get( currentUnit, potentialUnit, null, null, null );
  }

  public static ConductanceUnit get( CurrentUnit currentUnit, PotentialUnit potentialUnit, Rational factor, String name, String symbol )
  {
    ConductanceUnit unit = new ConductanceUnit( currentUnit, potentialUnit, factor, name, symbol );
    return CACHE.get( unit );
  }

  @Override
  public Conductance makeDimension( Number amount )
  {
    return new Conductance( Rational.get( amount ), this );
  }

  private ConductanceUnit( CurrentUnit currentUnit, PotentialUnit potentialUnit, Rational factor, String name, String symbol )
  {
    super( currentUnit, potentialUnit, factor, name, symbol );
  }

  public CurrentUnit getCurrentUnit()
  {
    return getLeftUnit();
  }

  public PotentialUnit getPotentialUnit()
  {
    return getRightUnit();
  }
}
