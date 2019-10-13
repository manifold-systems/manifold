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

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;

public final class Power extends AbstractMeasure<PowerUnit, Power>
{
  public Power( Rational value, PowerUnit unit, PowerUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Power( Rational value, PowerUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public PowerUnit getBaseUnit()
  {
    return PowerUnit.BASE;
  }

  @Override
  public Power make( Rational value, PowerUnit unit, PowerUnit displayUnit )
  {
    return new Power( value, unit, displayUnit );
  }

  @Override
  public Power make( Rational value, PowerUnit unit )
  {
    return new Power( value, unit );
  }

  public Energy times( Time time )
  {
    return new Energy( toBaseNumber() * time.toBaseNumber(), EnergyUnit.BASE, getDisplayUnit().getEnergyUnit() );
  }

  public Force div( Velocity v )
  {
    return new Force( toBaseNumber() / v.toBaseNumber(), ForceUnit.BASE, getDisplayUnit().getEnergyUnit().getForceUnit() );
  }

  public Velocity div( Force force )
  {
    return new Velocity( toBaseNumber() / force.toBaseNumber(), VelocityUnit.BASE, getDisplayUnit().getEnergyUnit().getForceUnit().getAccUnit().getVelocityUnit() );
  }

  public Current div( Potential potential )
  {
    return new Current( toBaseNumber() / potential.toBaseNumber(), CurrentUnit.BASE, potential.getDisplayUnit().getCurrentUnit() );
  }

  public Potential div( Current current )
  {
    return new Potential( toBaseNumber() / current.toBaseNumber(), PotentialUnit.BASE, PotentialUnit.get( getDisplayUnit(), current.getDisplayUnit() ) );
  }
}
