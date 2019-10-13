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


import static manifold.science.measures.LengthUnit.Inch;
import static manifold.science.measures.MassUnit.Kilogram;
import static manifold.science.measures.MassUnit.Pound;
import static manifold.science.util.CoercionConstants.r;

public final class PressureUnit extends AbstractQuotientUnit<MassUnit, AreaUnit, Pressure, PressureUnit>
{
  private static final UnitCache<PressureUnit> CACHE = new UnitCache<>();

  public static final PressureUnit Pa = get( Kilogram, AreaUnit.BASE, 1 r, "Pascal", "Pa" );
  public static final PressureUnit bar = get( Kilogram, AreaUnit.BASE, "1.0e5"r, "Bar", "bar" );
  public static final PressureUnit at = get( Kilogram, AreaUnit.BASE, "9.80665e4"r, "TechnicalAtm", "at" );
  public static final PressureUnit atm = get( Kilogram, AreaUnit.BASE, "1.01325e5"r, "StandardAtm", "atm" );
  public static final PressureUnit Torr = get( Kilogram, AreaUnit.BASE, "133.3224"r, "Torr", "Torr" );
  public static final PressureUnit psi = get( Pound, AreaUnit.get( Inch ), 1 r, "Psi", "psi" );

  public static final PressureUnit BASE = Pa;

  public static PressureUnit get( MassUnit massUnit, AreaUnit areaUnit )
  {
    return get( massUnit, areaUnit, null, null, null );
  }

  public static PressureUnit get( MassUnit massUnit, AreaUnit areaUnit, Rational factor, String name, String symbol )
  {
    PressureUnit unit = new PressureUnit( massUnit, areaUnit, factor, name, symbol );
    return CACHE.get( unit );
  }

  private PressureUnit( MassUnit massUnit, AreaUnit areaUnit, Rational factor, String name, String symbol )
  {
    super( massUnit, areaUnit, factor, name, symbol );
  }

  @Override
  public Pressure makeDimension( Number amount )
  {
    return new Pressure( Rational.get( amount ), this );
  }

  public MassUnit getMassUnit()
  {
    return getLeftUnit();
  }

  public AreaUnit getAreaUnit()
  {
    return getRightUnit();
  }
}