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

public final class Length extends AbstractMeasure<LengthUnit, Length>
{
  public Length( Rational value, LengthUnit unit, LengthUnit displayUnit ) {
    super( value, unit, displayUnit, LengthUnit.Meter );
  }
  public Length( Rational value, LengthUnit unit ) {
    this( value, unit, unit );
  }

  @Override
  public Length make( Rational value, LengthUnit unit, LengthUnit displayUnit ) {
    return new Length( value, unit, displayUnit );
  }
  @Override
  public Length make( Rational value, LengthUnit unit ) {
    return new Length( value, unit, unit );
  }

  public Velocity div( Time t ) {
    return new Velocity( toBaseNumber() / t.toBaseNumber(), VelocityUnit.BASE, VelocityUnit.get( getUnit(), t.getUnit() ) );
  }
  public Time div( Velocity v ) {
    return new Time( toBaseNumber() / v.toBaseNumber(), TimeUnit.BASE, v.getUnit().getTimeUnit() );
  }

  public Area times( Length len ) {
    return new Area( toBaseNumber() * len.toBaseNumber(), AreaUnit.BASE, AreaUnit.get( getUnit(), len.getUnit() ) );
  }

  public Volume times( Area area ) {
    return new Volume( toBaseNumber() * area.toBaseNumber(), VolumeUnit.BASE, VolumeUnit.get( getUnit(), area.getUnit() ) );
  }

  public Energy times( Force force ) {
    return new Energy( toBaseNumber() * force.toBaseNumber(), EnergyUnit.BASE, EnergyUnit.get( force.getUnit(), getUnit() ) );
  }
}
