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

public final class MagneticFluxDensityUnit extends AbstractQuotientUnit<MagneticFluxUnit, AreaUnit, MagneticFluxDensity, MagneticFluxDensityUnit>
{
  private static final UnitCache<MagneticFluxDensityUnit> CACHE = new UnitCache<>();

  public static final MagneticFluxDensityUnit T = get( MagneticFluxUnit.BASE, AreaUnit.BASE, 1 r, "Tesla", "T" );

  public static final MagneticFluxDensityUnit BASE = T;

  public static MagneticFluxDensityUnit get( MagneticFluxUnit magneticfluxUnit, AreaUnit areaUnit )
  {
    return get( magneticfluxUnit, areaUnit, null, null, null );
  }

  public static MagneticFluxDensityUnit get( MagneticFluxUnit magneticfluxUnit, AreaUnit areaUnit, Rational factor, String name, String symbol )
  {
    MagneticFluxDensityUnit unit = new MagneticFluxDensityUnit( magneticfluxUnit, areaUnit, factor, name, symbol );
    return CACHE.get( unit );
  }

  private MagneticFluxDensityUnit( MagneticFluxUnit magneticfluxUnit, AreaUnit areaUnit, Rational factor, String name, String symbol )
  {
    super( magneticfluxUnit, areaUnit, factor, name, symbol );
  }

  @Override
  public MagneticFluxDensity makeDimension( Number amount )
  {
    return new MagneticFluxDensity( Rational.get( amount ), this );
  }

  public MagneticFluxUnit getMagneticFluxUnit()
  {
    return getLeftUnit();
  }

  public AreaUnit getAreaUnit()
  {
    return getRightUnit();
  }
}
