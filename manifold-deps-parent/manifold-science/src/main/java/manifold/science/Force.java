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

public final class Force extends AbstractMeasure<ForceUnit, Force>
{
  public Force( Rational value, ForceUnit unit, ForceUnit displayUnit ) {
    super( value, unit, displayUnit, ForceUnit.BASE );
  }
  public Force( Rational value, ForceUnit unit ) {
    this( value, unit, unit );
  }

  @Override
  public Force make( Rational value, ForceUnit unit, ForceUnit displayUnit )
  {
    return new Force( value, unit, displayUnit );
  }
  @Override
  public Force make( Rational value, ForceUnit unit )
  {
    return new Force( value, unit );
  }

  public Power times( Velocity v ) {
    return new Power( toBaseNumber() * v.toBaseNumber(), PowerUnit.BASE, getUnit() * v.getUnit().getLengthUnit() / v.getUnit().getTimeUnit() );
  }

  public Energy times( Length len ) {
    return new Energy( toBaseNumber() * len.toBaseNumber(), EnergyUnit.BASE, getUnit() * len.getUnit() );
  }

  public Momentum times( Time t ) {
    return new Momentum( toBaseNumber() * t.toBaseNumber(), MomentumUnit.BASE, getUnit() * t.getUnit() );
  }

  public Acceleration div( Mass w ) {
    return new Acceleration( toBaseNumber() / w.toBaseNumber(), AccelerationUnit.BASE, getUnit().getAccUnit() );
  }
  public Mass div( Acceleration acc ) {
    return new Mass( toBaseNumber() / acc.toBaseNumber(), MassUnit.BASE, getUnit().getMassUnit() );
  }
}