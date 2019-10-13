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


import static manifold.science.measures.MetricScaleUnit.*;
import static manifold.science.util.CoercionConstants.r;

public final class EnergyUnit extends AbstractProductUnit<ForceUnit, LengthUnit, Energy, EnergyUnit>
{
  private static final UnitCache<EnergyUnit> CACHE = new UnitCache<>();

  public static EnergyUnit J = get( ForceUnit.BASE, LengthUnit.BASE, null, "Joule", "J" );
  public static EnergyUnit kJ = get( ForceUnit.BASE, LengthUnit.BASE, 1 k, "Kilojoule", "kJ" );
  public static EnergyUnit cal = get( ForceUnit.BASE, LengthUnit.BASE, "4.184"r, "Calorie", "cal" );
  public static EnergyUnit kcal = get( ForceUnit.BASE, LengthUnit.BASE, "4184"r, "Kilocalorie", "kcal" );
  public static EnergyUnit eV = get( ForceUnit.BASE, LengthUnit.BASE, "1.60217733e-19"r, "Electronvolt", "eV" );

  public static EnergyUnit BASE = J;

  public static EnergyUnit get( ForceUnit forceUnit, LengthUnit lengthUnit )
  {
    return get( forceUnit, lengthUnit, null, null, null );
  }

  public static EnergyUnit get( ForceUnit forceUnit, LengthUnit lengthUnit, Rational factor, String name, String symbol )
  {
    EnergyUnit unit = new EnergyUnit( forceUnit, lengthUnit, factor, name, symbol );
    return CACHE.get( unit );
  }

  private EnergyUnit( ForceUnit forceUnit, LengthUnit lengthUnit, Rational factor, String name, String symbol )
  {
    super( forceUnit, lengthUnit, factor, name, symbol );
  }

  @Override
  public Energy makeDimension( Number amount )
  {
    return new Energy( Rational.get( amount ), this );
  }

  public ForceUnit getForceUnit()
  {
    return getLeftUnit();
  }

  public LengthUnit getLengthUnit()
  {
    return getRightUnit();
  }

  public ForceUnit div( LengthUnit w )
  {
    return getForceUnit();
  }

  public PowerUnit div( TimeUnit time )
  {
    return PowerUnit.get( this, time );
  }

  public TimeUnit div( PowerUnit power )
  {
    return power.getTimeUnit();
  }

  public HeatCapacityUnit div( TemperatureUnit temperature )
  {
    return HeatCapacityUnit.get( this, temperature );
  }

  public TemperatureUnit div( HeatCapacityUnit c )
  {
    return c.getTemperatureUnit();
  }

  public MagneticFluxUnit div( CurrentUnit i )
  {
    return MagneticFluxUnit.get( this, i );
  }

  public CurrentUnit div( MagneticFluxUnit mf )
  {
    return mf.getCurrentUnit();
  }
}
