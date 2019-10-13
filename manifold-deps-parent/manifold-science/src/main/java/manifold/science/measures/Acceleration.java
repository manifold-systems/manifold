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

public final class Acceleration extends AbstractMeasure<AccelerationUnit, Acceleration>
{
  public Acceleration( Rational value, AccelerationUnit unit, AccelerationUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Acceleration( Rational value, AccelerationUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public AccelerationUnit getBaseUnit()
  {
    return AccelerationUnit.BASE;
  }

  @Override
  public Acceleration make( Rational value, AccelerationUnit unit, AccelerationUnit displayUnit )
  {
    return new Acceleration( value, unit, displayUnit );
  }

  @Override
  public Acceleration make( Rational value, AccelerationUnit unit )
  {
    return new Acceleration( value, unit );
  }

  public Force times( Mass mass )
  {
    return new Force( toBaseNumber() * mass.toBaseNumber(), ForceUnit.BASE, ForceUnit.get( mass.getDisplayUnit(), getDisplayUnit() ) );
  }

  public Velocity times( Time time )
  {
    return new Velocity( toBaseNumber() * time.toBaseNumber(), VelocityUnit.BASE, getDisplayUnit().getVelocityUnit() );
  }
}