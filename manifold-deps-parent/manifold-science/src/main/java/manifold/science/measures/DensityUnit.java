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


import static manifold.science.measures.MassUnit.Kilogram;

public final class DensityUnit extends AbstractQuotientUnit<MassUnit, VolumeUnit, Density, DensityUnit>
{
  private static final UnitCache<DensityUnit> CACHE = new UnitCache<>();

  public static final DensityUnit BASE = get( Kilogram, VolumeUnit.BASE );

  public static DensityUnit get( MassUnit massUnit, VolumeUnit volumeUnit )
  {
    return get( massUnit, volumeUnit, null, null, null );
  }

  public static DensityUnit get( MassUnit massUnit, VolumeUnit volumeUnit, Rational factor, String name, String symbol )
  {
    DensityUnit unit = new DensityUnit( massUnit, volumeUnit, factor, name, symbol );
    return CACHE.get( unit );
  }

  private DensityUnit( MassUnit massUnit, VolumeUnit volumeUnit, Rational factor, String name, String symbol )
  {
    super( massUnit, volumeUnit, factor, name, symbol );
  }

  @Override
  public Density makeDimension( Number amount )
  {
    return new Density( Rational.get( amount ), this );
  }

  public MassUnit getMassUnit()
  {
    return getLeftUnit();
  }

  public VolumeUnit getVolumeUnit()
  {
    return getRightUnit();
  }
}
