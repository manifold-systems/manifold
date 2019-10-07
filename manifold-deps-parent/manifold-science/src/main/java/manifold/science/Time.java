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


import static manifold.science.TimeUnit.Nano;

final public class Time extends AbstractMeasure<TimeUnit, Time>
{
  public Time( Rational value, TimeUnit unit, TimeUnit displayUnit ) {
    super( value, unit, displayUnit, TimeUnit.Second );
  }
  public Time( Rational value, TimeUnit unit ) {
    this( value, unit, unit );
  }

  @Override
  public Time make( Rational value, TimeUnit unit, TimeUnit displayUnit ) {
    return new Time( value, unit, displayUnit );
  }
  @Override
  public Time make( Rational value, TimeUnit unit ) {
    return new Time( value, unit, unit );
  }

  public static Time now() {
    return new Time( Rational.get( System.nanoTime() ), Nano );
  }

  public Length times( Velocity r ) {
    return new Length( toBaseNumber() * r.toBaseNumber(), LengthUnit.BASE, r.getUnit().getLengthUnit() );
  }

  public Velocity times( Acceleration acc ) {
    return new Velocity( toBaseNumber() * acc.toBaseNumber(), VelocityUnit.BASE, acc.getUnit().getVelocityUnit() );
  }

  public Charge times( Current current ) {
    return new Charge( toBaseNumber() * current.toBaseNumber(), ChargeUnit.BASE, current.getUnit().getChargeUnit() );
  }

  public Angle times( Frequency frequency ) {
    return new Angle( toBaseNumber() * frequency.toBaseNumber(), AngleUnit.BASE, frequency.getUnit().getAngleUnit() );
  }

  public Energy times( Power power ) {
    return new Energy( toBaseNumber() * power.toBaseNumber(), EnergyUnit.BASE, power.getUnit().getEnergyUnit() );
  }

  public Momentum times( Force force ) {
    return new Momentum( toBaseNumber() * force.toBaseNumber(), MomentumUnit.BASE, getUnit() * force.getUnit() );
  }
}
