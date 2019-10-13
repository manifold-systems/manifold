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

final public class Velocity extends AbstractMeasure<VelocityUnit, Velocity>
{
  public Velocity( Rational value, VelocityUnit unit, VelocityUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Velocity( Rational value, VelocityUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public VelocityUnit getBaseUnit()
  {
    return VelocityUnit.BASE;
  }

  @Override
  public Velocity make( Rational value, VelocityUnit unit, VelocityUnit displayUnit )
  {
    return new Velocity( value, unit, displayUnit );
  }

  @Override
  public Velocity make( Rational value, VelocityUnit unit )
  {
    return new Velocity( value, unit, unit );
  }

  public Length times( Time t )
  {
    return new Length( toBaseNumber() * t.toBaseNumber(), LengthUnit.BASE, getDisplayUnit().getLengthUnit() );
  }

  public Acceleration div( Time t )
  {
    return new Acceleration( toBaseNumber() / t.toBaseNumber(), AccelerationUnit.BASE, AccelerationUnit.get( getDisplayUnit(), t.getDisplayUnit() ) );
  }

  public Time div( Acceleration acc )
  {
    return new Time( toBaseNumber() / acc.toBaseNumber(), TimeUnit.BASE, acc.getDisplayUnit().getTimeUnit() );
  }

  public Momentum times( Mass mass )
  {
    return new Momentum( toBaseNumber() * mass.toBaseNumber(), MomentumUnit.BASE, MomentumUnit.get( mass.getDisplayUnit(), getDisplayUnit() ) );
  }

  public Power times( Force force )
  {
    return new Power( toBaseNumber() * force.toBaseNumber(), PowerUnit.BASE, getDisplayUnit() * force.getDisplayUnit() );
  }
}
