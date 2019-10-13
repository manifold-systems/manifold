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


import static manifold.science.util.CoercionConstants.r;

public final class InductanceUnit extends AbstractProductUnit<ResistanceUnit, TimeUnit, Inductance, InductanceUnit>
{
  private static final UnitCache<InductanceUnit> CACHE = new UnitCache<>();

  public static final InductanceUnit H = get( ResistanceUnit.BASE, TimeUnit.BASE, 1 r, "Henry", "H" );

  public static final InductanceUnit BASE = H;

  public static InductanceUnit get( ResistanceUnit resistanceUnit, TimeUnit timeUnit )
  {
    return get( resistanceUnit, timeUnit, null, null, null );
  }

  public static InductanceUnit get( ResistanceUnit resistanceUnit, TimeUnit timeUnit, Rational factor, String name, String symbol )
  {
    InductanceUnit unit = new InductanceUnit( resistanceUnit, timeUnit, factor, name, symbol );
    return CACHE.get( unit );
  }

  private InductanceUnit( ResistanceUnit resistanceUnit, TimeUnit timeUnit, Rational factor, String name, String symbol )
  {
    super( resistanceUnit, timeUnit, factor, name, symbol );
  }

  @Override
  public Inductance makeDimension( Number amount )
  {
    return new Inductance( Rational.get( amount ), this );
  }

  public ResistanceUnit getResistanceUnit()
  {
    return getLeftUnit();
  }

  public TimeUnit getTimeUnit()
  {
    return getRightUnit();
  }

  public ResistanceUnit div( TimeUnit w )
  {
    return getResistanceUnit();
  }
}
