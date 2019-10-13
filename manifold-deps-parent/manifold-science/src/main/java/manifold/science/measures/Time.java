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


import static manifold.science.measures.TimeUnit.Nano;

/**
 * Models a measure of time in units of {@link TimeUnit}.
 * <p/>
 * Use values of {@code Time} directly in arithmetic expressions with other dimensions such as:
 * <pre><code>
 *   // commonly used unit abbreviations e.g., m, ft, hr, mph, etc.
 *   import static manifold.science.util.UnitConstants.*;
 *   ...
 *   Time duration = 5.2 hr;
 *   Time mileTime = 4 min + 12.78 sec;
 *   Length distance = 80 mph * 2.3 hr;
 * </code></pre>
 */
final public class Time extends AbstractMeasure<TimeUnit, Time>
{
  public Time( Rational value, TimeUnit unit, TimeUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Time( Rational value, TimeUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public TimeUnit getBaseUnit()
  {
    return TimeUnit.BASE;
  }

  @Override
  public Time make( Rational value, TimeUnit unit, TimeUnit displayUnit )
  {
    return new Time( value, unit, displayUnit );
  }

  @Override
  public Time make( Rational value, TimeUnit unit )
  {
    return new Time( value, unit, unit );
  }

  public static Time now()
  {
    return new Time( Rational.get( System.nanoTime() ), Nano );
  }

  public Length times( Velocity r )
  {
    return new Length( toBaseNumber() * r.toBaseNumber(), LengthUnit.BASE, r.getDisplayUnit().getLengthUnit() );
  }

  public Velocity times( Acceleration acc )
  {
    return new Velocity( toBaseNumber() * acc.toBaseNumber(), VelocityUnit.BASE, acc.getDisplayUnit().getVelocityUnit() );
  }

  public Charge times( Current current )
  {
    return new Charge( toBaseNumber() * current.toBaseNumber(), ChargeUnit.BASE, current.getDisplayUnit().getChargeUnit() );
  }

  public Angle times( Frequency frequency )
  {
    return new Angle( toBaseNumber() * frequency.toBaseNumber(), AngleUnit.BASE, frequency.getDisplayUnit().getAngleUnit() );
  }

  public Energy times( Power power )
  {
    return new Energy( toBaseNumber() * power.toBaseNumber(), EnergyUnit.BASE, power.getDisplayUnit().getEnergyUnit() );
  }

  public Momentum times( Force force )
  {
    return new Momentum( toBaseNumber() * force.toBaseNumber(), MomentumUnit.BASE, getDisplayUnit() * force.getDisplayUnit() );
  }
}
