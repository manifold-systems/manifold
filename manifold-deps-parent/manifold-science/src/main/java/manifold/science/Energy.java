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

package manifold.science;

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;


import static manifold.science.LengthUnit.Meter;

public final class Energy extends AbstractMeasure<EnergyUnit, Energy>
{
  public Energy( Rational value, EnergyUnit unit, EnergyUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Energy( Rational value, EnergyUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public EnergyUnit getBaseUnit()
  {
    return EnergyUnit.BASE;
  }

  @Override
  public Energy make( Rational value, EnergyUnit unit, EnergyUnit displayUnit )
  {
    return new Energy( value, unit, displayUnit );
  }

  @Override
  public Energy make( Rational value, EnergyUnit unit )
  {
    return new Energy( value, unit );
  }

  public Length div( Force f )
  {
    return new Length( toBaseNumber() / f.toBaseNumber(), Meter, getDisplayUnit().getLengthUnit() );
  }

  public Force div( Length len )
  {
    return new Force( toBaseNumber() / len.toBaseNumber(), ForceUnit.BASE, getDisplayUnit().getForceUnit() );
  }

  public Power div( Time t )
  {
    return new Power( toBaseNumber() / t.toBaseNumber(), PowerUnit.BASE, PowerUnit.get( getDisplayUnit(), t.getDisplayUnit() ) );
  }

  public Time div( Power power )
  {
    return new Time( toBaseNumber() / power.toBaseNumber(), TimeUnit.BASE, power.getDisplayUnit().getTimeUnit() );
  }

  public HeatCapacity div( Temperature temperature )
  {
    return new HeatCapacity( toBaseNumber() / temperature.toBaseNumber(), HeatCapacityUnit.BASE, getDisplayUnit() / temperature.getDisplayUnit() );
  }

  public Temperature div( HeatCapacity c )
  {
    return new Temperature( toBaseNumber() / c.toBaseNumber(), TemperatureUnit.BASE, c.getDisplayUnit().getTemperatureUnit() );
  }

  public MagneticFlux div( Current i )
  {
    return new MagneticFlux( toBaseNumber() / i.toBaseNumber(), MagneticFluxUnit.BASE, MagneticFluxUnit.get( getDisplayUnit(), i.getDisplayUnit() ) );
  }

  public Current div( MagneticFlux mf )
  {
    return new Current( toBaseNumber() / mf.toBaseNumber(), CurrentUnit.BASE, mf.getDisplayUnit().getCurrentUnit() );
  }
}
