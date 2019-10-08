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

import manifold.science.api.Unit;
import manifold.science.util.Rational;


import static manifold.science.MetricScaleUnit.*;
import static manifold.science.util.CoercionConstants.r;

public enum TimeUnit implements Unit<Time, TimeUnit>
{
  // Ephemeris (SI) units
  Planck( "5.39056e-44"r, "Planck-time", "tP" ),
  Femto( 1 fe, "Femtosecond", "fs" ),
  Pico( 1p, "Picosecond", "ps" ),
  Nano( 1n, "Nanosecond", "ns" ),
  Micro( 1u, "Microsecond", "µs" ),
  Milli( 1m, "Millisecond", "ms" ),
  Second( 1r, "Second", "s" ),
  Minute( 60r, "Minute", "min"  ),
  Hour( 60r*60, "Hour", "hr" ),
  Day( 24r*60*60, "Day", "day" ),
  Week( 7r*24*60*60, "Week", "wk" ),

  // Mean Gregorian (ISO Calendar) units
  Month( 31556952r/12, "Month", "mo" ),
  Year( 31556952r, "Year", "yr" ),
  Decade( 31556952r * 10, "Decade", "decade" ),
  Century( 31556952r * 100, "Century", "century" ),
  Millennium( 31556952 k, "Millennium", "millennium" ),
  Era( 31556952 G, "Era", "era" ),

  // Mean Tropical (Solar) units
  TrMonth( "31556925.445/12"r, "Tropical Month", "tmo" ),
  TrYear( "31556925.445"r, "Tropical Year", "tyr" );

  public static final TimeUnit BASE = Second;

  private Rational _sec;
  private String _name;
  private String _symbol;


  TimeUnit( Rational sec, String name, String symbol ) {
    _sec = sec;
    _name = name;
    _symbol = symbol;
  }

  public String getUnitName() {
    return _name;
  }

  public String getUnitSymbol() {
    return _symbol;
  }

  public Rational toBaseUnits( Rational myUnits ) {
    return _sec * myUnits;
  }

  @Override
  public Rational from( Time t )
  {
    return t.toBaseNumber() / _sec;
  }

  @Override
  public Time makeDimension( Number amount )
  {
    return new Time( Rational.get( amount ), this );
  }

  public Rational toNumber() {
    return _sec;
  }

  public Rational getSeconds()
  {
    return _sec;
  }


  public LengthUnit times( VelocityUnit v ) {
    return v.getLengthUnit();
  }

  public VelocityUnit times( AccelerationUnit acc ) {
    return acc.getVelocityUnit();
  }

  public ChargeUnit times( CurrentUnit current ) {
    return ChargeUnit.Coulomb;
  }

  public AngleUnit times( FrequencyUnit frequency ) {
    return frequency.getAngleUnit();
  }

  public EnergyUnit times( PowerUnit power ) {
    return power.getEnergyUnit();
  }

  public MomentumUnit times( ForceUnit force ) {
    return force.getMassUnit() * (force.getAccUnit().getVelocityUnit().getLengthUnit()/this);
  }
}