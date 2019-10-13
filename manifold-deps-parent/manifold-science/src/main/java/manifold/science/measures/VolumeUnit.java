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


import static manifold.science.measures.LengthUnit.Centi;
import static manifold.science.measures.LengthUnit.Meter;
import static manifold.science.util.CoercionConstants.r;
import static manifold.science.util.MetricFactorConstants.KILO;

public final class VolumeUnit extends AbstractProductUnit<LengthUnit, AreaUnit, Volume, VolumeUnit>
{
  private static final UnitCache<VolumeUnit> CACHE = new UnitCache<>();

  public static final VolumeUnit BASE = get( Meter, AreaUnit.get( Meter ) );

  public static final VolumeUnit LITER = get( Centi, AreaUnit.get( Centi ), KILO, "Litre", "L" );
  public static final VolumeUnit MILLI_LITER = get( Centi, AreaUnit.get( Centi ), 1 r, "Millilitre", "mL" );
  public static final VolumeUnit FLUID_OZ = get( Centi, AreaUnit.get( Centi ), 29.5735295625r, "Fluid Ounce", "fl oz." );
  public static final VolumeUnit GALLON = get( Centi, AreaUnit.get( Centi ), 3785.411784r, "Gallon", "gal." );
  public static final VolumeUnit QUART = get( Centi, AreaUnit.get( Centi ), 946.352946r, "Quart", "qt." );
  public static final VolumeUnit PINT = get( Centi, AreaUnit.get( Centi ), 473.176473r, "Pint", "pt." );
  public static final VolumeUnit CUP = get( Centi, AreaUnit.get( Centi ), 236.5882365r, "Cup", "c." );
  public static final VolumeUnit TABLE_SPOON = get( Centi, AreaUnit.get( Centi ), 14.78676478125r, "Tablespoon", "tbsp" );
  public static final VolumeUnit TEA_SPOON = get( Centi, AreaUnit.get( Centi ), 4.92892159375r, "Teaspoon", "tsp" );

  private final String _symbolProvided;

  public static VolumeUnit get( LengthUnit lengthUnit, AreaUnit areaUnit )
  {
    return get( lengthUnit, areaUnit, null, null, null );
  }

  public static VolumeUnit get( LengthUnit lengthUnit, AreaUnit areaUnit, Rational factor, String name, String symbol )
  {
    VolumeUnit unit = new VolumeUnit( lengthUnit, areaUnit, factor, name, symbol );
    return CACHE.get( unit );
  }

  private VolumeUnit( LengthUnit lengthUnit, AreaUnit areaUnit, Rational factor, String name, String symbol )
  {
    super( lengthUnit, areaUnit == null ? AreaUnit.get( lengthUnit ) : areaUnit, factor, name, symbol );
    _symbolProvided = symbol;
  }

  @Override
  public Volume makeDimension( Number amount )
  {
    return new Volume( Rational.get( amount ), this );
  }

  public AreaUnit getAreaUnit()
  {
    return getRightUnit();
  }

  public LengthUnit getLengthUnit()
  {
    return getLeftUnit();
  }

  public String getFullName()
  {
    return getAreaUnit().isSquare() && getAreaUnit().getWidthUnit() == getLengthUnit()
           ? getLengthUnit().getFullName() + "\u00B3"
           : getAreaUnit().getFullName() + "\u00D7" + getLengthUnit().getFullName();
  }

  public String getFullSymbol()
  {
    return getAreaUnit().isSquare() && getAreaUnit().getWidthUnit() == getLengthUnit()
           ? getLengthUnit().getFullSymbol() + "\u00B3"
           : getAreaUnit().getFullSymbol() + "\u00D7" + getLengthUnit().getFullSymbol();
  }

  @Override
  public String getSymbol()
  {
    if( _symbolProvided != null )
    {
      return _symbolProvided;
    }

    return getAreaUnit().isSquare() && getAreaUnit().getWidthUnit() == getLengthUnit()
           ? getLengthUnit().getSymbol() + "\u00B3"
           : super.getSymbol();
  }

  public boolean getIsCubic()
  {
    return getAreaUnit().isSquare() && getAreaUnit().getWidthUnit() == getLengthUnit();
  }

  public LengthUnit div( AreaUnit len )
  {
    return getLengthUnit();
  }

  public MassUnit times( DensityUnit density )
  {
    return density.getMassUnit();
  }
}
