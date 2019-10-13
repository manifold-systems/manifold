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

public final class HeatCapacityUnit extends AbstractQuotientUnit<EnergyUnit, TemperatureUnit, HeatCapacity, HeatCapacityUnit>
{
  private static final UnitCache<HeatCapacityUnit> CACHE = new UnitCache<>();

  public static final HeatCapacityUnit BASE = get( EnergyUnit.BASE, TemperatureUnit.BASE );
  public static final HeatCapacityUnit kB = get( EnergyUnit.BASE, TemperatureUnit.BASE, 1.380649e-23r, "Boltzmann-constant", "kB" );

  public static HeatCapacityUnit get( EnergyUnit energyUnit, TemperatureUnit temperatureUnit )
  {
    return get( energyUnit, temperatureUnit, null, null, null );
  }

  public static HeatCapacityUnit get( EnergyUnit energyUnit, TemperatureUnit temperatureUnit, Rational factor, String name, String symbol )
  {
    HeatCapacityUnit unit = new HeatCapacityUnit( energyUnit, temperatureUnit, factor, name, symbol );
    return CACHE.get( unit );
  }

  private HeatCapacityUnit( EnergyUnit energyUnit, TemperatureUnit temperatureUnit, Rational factor, String name, String symbol )
  {
    super( energyUnit, temperatureUnit, factor, name, symbol );
  }

  @Override
  public HeatCapacity makeDimension( Number amount )
  {
    return new HeatCapacity( Rational.get( amount ), this );
  }

  public EnergyUnit getEnergyUnit()
  {
    return getLeftUnit();
  }

  public TemperatureUnit getTemperatureUnit()
  {
    return getRightUnit();
  }
}
