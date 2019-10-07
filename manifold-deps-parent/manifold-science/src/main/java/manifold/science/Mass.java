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


import static manifold.science.MassUnit.Kilogram;

public final class Mass extends AbstractMeasure<MassUnit, Mass>
{
  public Mass( Rational value, MassUnit unit, MassUnit displayUnit ) {
    super( value, unit, displayUnit, Kilogram );
  }
  public Mass( Rational value, MassUnit unit ) {
    this( value, unit, unit );
  }

  @Override
  public Mass make( Rational value, MassUnit unit, MassUnit displayUnit )
  {
    return new Mass( value, unit, displayUnit );
  }
  @Override
  public Mass make( Rational value, MassUnit unit )
  {
    return new Mass( value, unit );
  }

  public Force times( Acceleration a ) {
    return new Force( toBaseNumber() * a.toBaseNumber(), ForceUnit.BASE, ForceUnit.get( getUnit(), a.getUnit() ) );
  } 
  
  public Momentum times( Velocity v ) {
    return new Momentum( toBaseNumber() * v.toBaseNumber(), MomentumUnit.BASE, MomentumUnit.get( getUnit(), v.getUnit() ) );
  }

  public Pressure div( Area area ) {
    return new Pressure( toBaseNumber() / area.toBaseNumber(), PressureUnit.BASE, PressureUnit.get( getUnit(), area.getUnit() ) );
  }
  public Area div( Pressure p ) {
    return new Area( toBaseNumber() / p.toBaseNumber(), AreaUnit.BASE, p.getUnit().getAreaUnit() );
  }

  public Density div( Volume volume ) {
    return new Density( toBaseNumber() / volume.toBaseNumber(), DensityUnit.BASE, DensityUnit.get( getUnit(), volume.getUnit() ) );
  }
  public Volume div( Density d ) {
    return new Volume( toBaseNumber() / d.toBaseNumber(), VolumeUnit.BASE, d.getUnit().getVolumeUnit() );
  }
}
