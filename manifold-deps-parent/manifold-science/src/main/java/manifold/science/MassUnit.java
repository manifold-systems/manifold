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

import manifold.science.api.IUnit;
import manifold.science.util.Rational;


import static manifold.science.MetricScaleUnit.*;

public enum MassUnit implements IUnit<Mass, MassUnit>
{
  AtomicMass( "1.6605402e-27"r, "AMU", "amu" ),
  Nano( 1p, "Nanogram", "µg" ),
  Micro( 1n, "Microgram", "µg" ),
  Milli( 1u, "Milligram", "mg" ),
  Gram( 1m, "Gram", "g" ),
  Kilogram( 1r, "Kilogram", "kg" ),
  Tonne( 1k, "Metric Ton", "tonne" ),
  Carat( ".0002"r, "Carat", "ct" ),
  Dram( ".001771845195312"r, "Dram", "dr" ),
  Grain( "6.47989e-5"r, "Grain", "gr" ),
  Newton( "0.101971621"r, "Newton", "N" ),
  Ounce( "0.0283495"r, "Ounce", "oz" ),
  TroyOunce( "0.0311035"r, "Troy Ounce", "ozt" ),
  Pound( "0.453592"r, "Pound", "lb" ),
  Stone( "6.35029"r, "Stone", "st" ),
  Ton( "907.185"r, "Ton (US, short)", "sht" ),
  TonUK( "1016.05"r, "Ton (UK, long)", "lt" ),
  Solar( "1.9889200011445836e30"r, "Solar Masses", "M☉" );

  public static final MassUnit BASE = Kilogram;
  
  private Rational _kilograms; // as Kilograms
  private String _name;
  private String _symbol;

  MassUnit( Rational kilograms, String name, String symbol ) {
    _kilograms = kilograms;
    _name = name;
    _symbol = symbol;
  }

  @Override
  public Mass makeDimension( Number amount )
  {
    return new Mass( Rational.get( amount ), this );
  }

  public String getUnitName() {
    return _name;
  }

  public String getUnitSymbol() {
    return _symbol;
  }

  public Rational toBaseUnits( Rational myUnits ) {
    return _kilograms * myUnits;
  }

  public Rational toNumber() {
    return _kilograms;
  }

  public Rational from( Mass w ) {
    return w.toBaseNumber() / _kilograms;
  }

  public MomentumUnit multiply( VelocityUnit velocity ) {
    return MomentumUnit.get( this, velocity );
  }

  public ForceUnit multiply( AccelerationUnit acc ) {
    return ForceUnit.get( this, acc );
  }

  public PressureUnit divide( AreaUnit area ) {
    return PressureUnit.get( this, area );
  }
  public AreaUnit divide( PressureUnit pressure ) {
    return pressure.getAreaUnit();
  }

  public DensityUnit divide( VolumeUnit volume ) {
    return DensityUnit.get( this, volume );
  }
  public VolumeUnit divide( DensityUnit d ) {
    return d.getVolumeUnit();
  }
}
