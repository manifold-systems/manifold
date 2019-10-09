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


import static manifold.science.util.CoercionConstants.r;
import static manifold.science.util.CommonConstants.*;

public enum LengthUnit implements Unit<Length, LengthUnit>
{
  // Planck length
  Planck( "1.61605e-35"r, "Planck-length", "ℓP" ),

  // Metric
  Femto( FEMTO, "Femtometer", "fm" ),
  Pico( PICO, "Picometer", "pm" ),
  Angstrom( Rational.get( "1e-10" ), "Angstrom", "Å" ),
  Nano( NANO, "Nanometer", "nm" ),
  Micro( MICRO, "Micrometre", "µm" ),
  Milli( MILLI, "Millimeter", "mm" ),
  Centi( CENTI, "Centimeter", "cm" ),
  Deci( DECI, "Decimeter", "dm" ),
  Meter( Rational.ONE, "Meter", "m" ),
  Kilometer( KILO, "Kilometer", "km" ),
  Megameter( KILO.pow( 2 ), "Megameter", "Mm" ),
  Gigameter( KILO.pow( 3 ), "Gigameter", "Gm" ),
  Terameter( KILO.pow( 4 ), "Terameter", "Tm" ),

  // UK
  Cubit( Rational.get( "0.4572" ), "Cubit", "cbt" ),

  // US Standard
  Caliber( ".000254"r, "Caliber", "cal." ),
  Inch( "0.0254"r, "Inch", "in" ),
  Foot( 12 * "0.0254"r, "Foot", "ft" ),
  Yard( 3 * 12 * "0.0254"r, "Yard", "yd" ),
  Rod( "5.0292"r, "Rod", "rd" ),
  Chain( "20.1168"r, "Chain", "ch" ),
  Furlong( "201.168"r, "Furlong", "fur" ),
  Mile( "1609.344"r, "Mile", "mi" ),

  // International
  NauticalMile( Rational.get( 1852 ), "NauticalMile", "n.m." ),

  // Very large units
  IAU( Rational.get( "1.49597870e11" ), "IAU-length", "au" ),
  LightYear( Rational.get( "9.460730473e+15" ), "LightYear", "ly" );

  public static final LengthUnit BASE = Meter;

  final Rational _meters;
  final String _name;
  final String _symbol;


  LengthUnit( Rational meters, String name, String symbol )
  {
    _meters = meters;
    _name = name;
    _symbol = symbol;
  }

  public Rational getMeters()
  {
    return _meters;
  }

  public String getUnitName()
  {
    return _name;
  }

  public String getUnitSymbol()
  {
    return _symbol;
  }

  public Rational toBaseUnits( Rational myUnits )
  {
    return _meters * myUnits;
  }

  public Rational toNumber()
  {
    return _meters;
  }

  public Rational from( Length len )
  {
    return len.toBaseNumber() / _meters;
  }

  @Override
  public Length makeDimension( Number amount )
  {
    return new Length( Rational.get( amount ), this );
  }

  public EnergyUnit postfixBind( ForceUnit f )
  {
    return times( f );
  }

  public VelocityUnit div( TimeUnit t )
  {
    return VelocityUnit.get( this, t );
  }

  public TimeUnit div( VelocityUnit v )
  {
    return v.getTimeUnit();
  }

  public AreaUnit times( LengthUnit len )
  {
    return AreaUnit.get( this, len );
  }

  public VolumeUnit times( AreaUnit area )
  {
    return VolumeUnit.get( this, area );
  }

  public EnergyUnit times( ForceUnit f )
  {
    return EnergyUnit.get( f, this );
  }
}
