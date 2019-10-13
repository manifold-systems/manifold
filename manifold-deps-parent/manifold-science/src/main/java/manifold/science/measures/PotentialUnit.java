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

public final class PotentialUnit extends AbstractQuotientUnit<PowerUnit, CurrentUnit, Potential, PotentialUnit>
{
  private static final UnitCache<PotentialUnit> CACHE = new UnitCache<>();

  public static final PotentialUnit V = get( PowerUnit.BASE, CurrentUnit.BASE, 1 r, "Volt", "V" );

  public static final PotentialUnit BASE = V;

  public static PotentialUnit get( PowerUnit powerUnit, CurrentUnit currentUnit )
  {
    return get( powerUnit, currentUnit, null, null, null );
  }

  public static PotentialUnit get( PowerUnit powerUnit, CurrentUnit currentUnit, Rational factor, String name, String symbol )
  {
    PotentialUnit unit = new PotentialUnit( powerUnit, currentUnit, factor, name, symbol );
    return CACHE.get( unit );
  }

  private PotentialUnit( PowerUnit powerUnit, CurrentUnit currentUnit, Rational factor, String name, String symbol )
  {
    super( powerUnit, currentUnit, factor, name, symbol );
  }

  @Override
  public Potential makeDimension( Number amount )
  {
    return new Potential( Rational.get( amount ), this );
  }

  public PowerUnit getPowerUnit()
  {
    return getLeftUnit();
  }

  public CurrentUnit getCurrentUnit()
  {
    return getRightUnit();
  }

  public ResistanceUnit div( CurrentUnit current )
  {
    return ResistanceUnit.get( this, current );
  }
}
