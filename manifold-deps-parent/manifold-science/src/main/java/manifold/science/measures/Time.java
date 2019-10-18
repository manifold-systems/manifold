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

import java.math.BigInteger;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.List;
import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;


import static java.time.temporal.ChronoUnit.NANOS;
import static java.time.temporal.ChronoUnit.SECONDS;
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
 * {@code Time} implements {@link TemporalAmount} so you can easily work with Java's {@link java.time} library:
 * <code><pre>
 *   LocalDateTime date = LocalDateTime.of( 2018, 10, 17, 17, 35 );
 *   LocalDateTime yearLater = date + 1 yr;
 *   LocalDateTime tomorrowOneHourLater = date + 1 day + 1 hr;
 * </pre></code>
 */
final public class Time extends AbstractMeasure<TimeUnit, Time> implements TemporalAmount
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

  //
  // TemporalAmount impl
  //

  @Override
  public long get( TemporalUnit unit )
  {
    if( unit == SECONDS )
    {
      return toBaseNumber().wholePart().longValue();
    }
    else if( unit == NANOS )
    {
      return toBaseNumber().fractionPart().times( 1.0e9 ).longValue();
    }
    else
    {
      throw new UnsupportedTemporalTypeException( "Unsupported unit: " + unit );
    }
  }

  @Override
  public List<TemporalUnit> getUnits()
  {
    return getBaseUnit().getDuration().getUnits();
  }

  @Override
  public Temporal addTo( Temporal temporal )
  {
    BigInteger wholePart = toBaseNumber().wholePart();
    if( !wholePart.equals( BigInteger.ZERO ) )
    {
      temporal = temporal.plus( wholePart.longValue(), SECONDS );
    }
    Rational fractionPart = toBaseNumber().fractionPart();
    if( !fractionPart.equals( Rational.ZERO ) )
    {
      temporal = temporal.plus( fractionPart.times( 1.0e9 ).longValue(), NANOS );
    }
    return temporal;
  }

  @Override
  public Temporal subtractFrom( Temporal temporal )
  {
    BigInteger wholePart = toBaseNumber().wholePart();
    if( !wholePart.equals( BigInteger.ZERO ) )
    {
      temporal = temporal.minus( wholePart.longValue(), SECONDS );
    }
    Rational fractionPart = toBaseNumber().fractionPart();
    if( !fractionPart.equals( Rational.ZERO ) )
    {
      temporal = temporal.minus( fractionPart.times( 1.0e9 ).longValue(), NANOS );
    }
    return temporal;
  }


  //
  // Operators
  //

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
