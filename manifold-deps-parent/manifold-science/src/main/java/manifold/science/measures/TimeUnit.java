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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import manifold.science.api.AbstractPrimaryUnit;
import manifold.science.api.UnitCache;
import manifold.science.util.Rational;


import static manifold.science.measures.MetricScaleUnit.*;
import static manifold.science.util.CoercionConstants.r;

/**
 * The second is the SI unit of time. All instances of {@code TimeUnit} are a factor of one second.
 * <p/>
 * <i>Atomic clocks, which keep time using transition energies in atoms, revolutionised timekeeping. NPL developed the
 * first operational caesium-beam atomic clock in 1955. This clock was so accurate that it would only gain or lose one
 * second in three hundred years. Modern atomic clocks can be as much as a million times more accurate than this, and
 * underpin satellite technology, like GPS or the internet.</i>
 * (ref. <a href="https://www.npl.co.uk/si-units/second">npl.co.uk</a>)
 */
public final class TimeUnit extends AbstractPrimaryUnit<Time, TimeUnit> implements TemporalUnit
{
  private static final UnitCache<TimeUnit> CACHE = new UnitCache<>();

  /**
   * Get or create a unit based on the {@code secondFactor}, which is a factor of the duration of one second. The
   * specified unit is cached and will be returned for subsequent calls to this method if the {@code secondFactor}
   * matches.
   * <p/>
   * @param secondFactor A factor of the duration of one second expressed as a {@link Rational}
   * @param duration A factor of the duration of one second expressed as a {@link Duration}(for compatibility with {@link java.time}).
   * @param fuzzyDuration {@code true} if the {@code duration} is possibly inexact.
   * @param isDateBased {@code true} if the {@code duration} is date oriented i.e., is a function of non-constant spatial properties related to a calendar.
   * @param name The standard full name of the unit e.g., "Hour".
   * @param symbol The standard symbol used for the unit e.g., "hr".
   * @return The specified unit.
   */
  public static TimeUnit get( Rational secondFactor, Duration duration, boolean fuzzyDuration, boolean isDateBased, String name, String symbol )
  {
    return CACHE.get( new TimeUnit( secondFactor, duration, fuzzyDuration, isDateBased, name, symbol ) );
  }

  // SI Units
  public static final TimeUnit Femto = get( 1 fe, Duration.ofNanos( 0 ), true, false, "Femtosecond", "fs" );
  public static final TimeUnit Pico = get( 1 p, Duration.ofNanos( 0 ), true, false, "Picosecond", "ps" );
  public static final TimeUnit Nano = get( 1 n, ChronoUnit.NANOS.getDuration(), false, false, "Nanosecond", "ns" );
  public static final TimeUnit Micro = get( 1 u, ChronoUnit.MICROS.getDuration(), false, false, "Microsecond", "µs" );
  public static final TimeUnit Milli = get( 1 m, ChronoUnit.MILLIS.getDuration(), false, false, "Millisecond", "ms" );
  public static final TimeUnit Second = get( 1 r, ChronoUnit.SECONDS.getDuration(), false, false, "Second", "s" );
  public static final TimeUnit Minute = get( 60 r, ChronoUnit.MINUTES.getDuration(), false, false, "Minute", "min" );
  public static final TimeUnit Hour = get( 60 r * 60, ChronoUnit.HOURS.getDuration(), false, false, "Hour", "hr" );
  public static final TimeUnit Day = get( 24 r * 60 * 60, ChronoUnit.DAYS.getDuration(), true, true, "Day", "day" );
  public static final TimeUnit Week = get( 7 r * 24 * 60 * 60, ChronoUnit.WEEKS.getDuration(), true, true, "Week", "wk" );

  // Mean Gregorian (ISO Calendar) units
  public static final TimeUnit Month = get( 31556952 r / 12, ChronoUnit.MONTHS.getDuration(), true, true, "Month", "mo" );
  public static final TimeUnit Year = get( 31556952 r, ChronoUnit.YEARS.getDuration(), true, true, "Year", "yr" );
  public static final TimeUnit Decade = get( 31556952 r * 10, ChronoUnit.DECADES.getDuration(), true, true, "Decade", "decade" );
  public static final TimeUnit Century = get( 31556952 r * 100, ChronoUnit.CENTURIES.getDuration(), true, true, "Century", "century" );
  public static final TimeUnit Millennium = get( 31556952 k, ChronoUnit.MILLENNIA.getDuration(), true, true, "Millennium", "millennium" );
  public static final TimeUnit Era = get( 31556952 G, ChronoUnit.ERAS.getDuration(), true, true, "Era", "era" );

  // Mean Tropical (Solar) units
  public static final TimeUnit TrMonth = get( "31556925.445/12"r, Duration.ofSeconds( 2629743, 787080000 ), true, true, "Tropical Month", "tmo" );
  public static final TimeUnit TrYear = get( "31556925.445"r, Duration.ofSeconds( 31556925, (long)4.45e8 ), true, true, "Tropical Year", "tyr" );

  // Planck-time
  public static final TimeUnit Planck = get( 5.39056e-44r, Duration.ofNanos( 0 ), true, false, "Planck-time", "tP" );

  public static final TimeUnit BASE = Second;

  // For compatibility with java.time
  private final Duration _duration;
  private final boolean _fuzzyDuration;
  private final boolean _isDateBased;

  private TimeUnit( Rational sec, Duration duration, boolean fuzzyDuration, boolean isDateBase, String name, String symbol )
  {
    super( sec, name, symbol );
    _duration = duration;
    _fuzzyDuration = fuzzyDuration;
    _isDateBased = isDateBase;
  }

  @Override
  public Time makeDimension( Number amount )
  {
    return new Time( Rational.get( amount ), this );
  }

  public Rational getSeconds()
  {
    return toNumber();
  }

  //
  // TemporalUnit Impl
  //

  @Override
  public Duration getDuration()
  {
    return _duration;
  }

  @Override
  public boolean isDurationEstimated()
  {
    return _fuzzyDuration;
  }

  @Override
  public boolean isDateBased()
  {
    return _isDateBased;
  }

  @Override
  public boolean isTimeBased()
  {
    return !_isDateBased;
  }

  @Override
  public <R extends Temporal> R addTo( R temporal, long amount )
  {
    return (R)temporal.plus( amount, this );
  }

  @Override
  public long between( Temporal temporal1Inclusive, Temporal temporal2Exclusive )
  {
    return temporal1Inclusive.until( temporal2Exclusive, this );
  }

  //
  // Operators
  //

  public LengthUnit times( VelocityUnit v )
  {
    return v.getLengthUnit();
  }

  public VelocityUnit times( AccelerationUnit acc )
  {
    return acc.getVelocityUnit();
  }

  public ChargeUnit times( CurrentUnit current )
  {
    return ChargeUnit.Coulomb;
  }

  public AngleUnit times( FrequencyUnit frequency )
  {
    return frequency.getAngleUnit();
  }

  public EnergyUnit times( PowerUnit power )
  {
    return power.getEnergyUnit();
  }

  public MomentumUnit times( ForceUnit force )
  {
    return force.getMassUnit() * (force.getAccUnit().getVelocityUnit().getLengthUnit() / this);
  }
}