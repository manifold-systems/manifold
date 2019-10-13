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


import static manifold.science.measures.AngleUnit.Radian;
import static manifold.science.measures.AngleUnit.Turn;
import static manifold.science.util.CoercionConstants.r;
import static manifold.science.measures.TimeUnit.Minute;
import static manifold.science.measures.TimeUnit.Second;

public final class FrequencyUnit extends AbstractQuotientUnit<AngleUnit, TimeUnit, Frequency, FrequencyUnit>
{
  private static final UnitCache<FrequencyUnit> CACHE = new UnitCache<>();

  public static final FrequencyUnit BASE = get( Radian, Second );
  public static final FrequencyUnit Hertz = get( Turn, Second, 1 r, "Hertz", "Hz" );
  public static final FrequencyUnit RPM = get( Turn, Minute, 1 r, "RPM", "rpm" );

  public static FrequencyUnit get( AngleUnit angleUnit, TimeUnit timeUnit )
  {
    return get( angleUnit, timeUnit, null, null, null );
  }

  public static FrequencyUnit get( AngleUnit angleUnit, TimeUnit timeUnit, Rational factor, String name, String symbol )
  {
    FrequencyUnit unit = new FrequencyUnit( angleUnit, timeUnit, factor, name, symbol );
    return CACHE.get( unit );
  }

  private FrequencyUnit( AngleUnit angleUnit, TimeUnit timeUnit, Rational factor, String name, String symbol )
  {
    super( angleUnit, timeUnit, factor, name, symbol );
  }

  @Override
  public Frequency makeDimension( Number amount )
  {
    return new Frequency( Rational.get( amount ), this );
  }

  public AngleUnit getAngleUnit()
  {
    return getLeftUnit();
  }

  public TimeUnit getTimeUnit()
  {
    return getRightUnit();
  }
}
