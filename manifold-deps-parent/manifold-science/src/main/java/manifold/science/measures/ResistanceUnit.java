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

public final class ResistanceUnit extends AbstractQuotientUnit<PotentialUnit, CurrentUnit, Resistance, ResistanceUnit>
{
  private static final UnitCache<ResistanceUnit> CACHE = new UnitCache<>();

  public static final ResistanceUnit ohm = get( PotentialUnit.BASE, CurrentUnit.BASE, 1 r, "Ohm", "Ω" );

  public static final ResistanceUnit BASE = ohm;

  public static ResistanceUnit get( PotentialUnit potentialUnit, CurrentUnit currentUnit )
  {
    return get( potentialUnit, currentUnit, null, null, null );
  }

  public static ResistanceUnit get( PotentialUnit potentialUnit, CurrentUnit currentUnit, Rational factor, String name, String symbol )
  {
    ResistanceUnit unit = new ResistanceUnit( potentialUnit, currentUnit, factor, name, symbol );
    return CACHE.get( unit );
  }

  private ResistanceUnit( PotentialUnit potentialUnit, CurrentUnit currentUnit, Rational factor, String name, String symbol )
  {
    super( potentialUnit, currentUnit, factor, name, symbol );
  }

  @Override
  public Resistance makeDimension( Number amount )
  {
    return new Resistance( Rational.get( amount ), this );
  }

  public PotentialUnit getPotentialUnit()
  {
    return getLeftUnit();
  }

  public CurrentUnit getCurrentUnit()
  {
    return getRightUnit();
  }

  public InductanceUnit times( TimeUnit t )
  {
    return InductanceUnit.get( this, t );
  }
}
