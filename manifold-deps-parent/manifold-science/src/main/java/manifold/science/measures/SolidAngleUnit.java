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

public final class SolidAngleUnit extends AbstractPrimaryUnit<SolidAngle, SolidAngleUnit>
{
  private static final UnitCache<SolidAngleUnit> CACHE = new UnitCache<>();
  public static SolidAngleUnit get( Rational seconds, String name, String symbol )
  {
    return CACHE.get( new SolidAngleUnit( seconds, name, symbol ) );
  }

  public static final SolidAngleUnit Steradian = get( 1r, "Steradian", "sr" );

  public static final SolidAngleUnit BASE = Steradian;

  private SolidAngleUnit( Rational sr, String name, String symbol )
  {
    super( sr, name, symbol );
  }

  @Override
  public SolidAngle makeDimension( Number amount )
  {
    return new SolidAngle( Rational.get( amount ), this );
  }
}
