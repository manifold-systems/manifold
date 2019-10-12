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

import manifold.science.api.AbstractPrimaryUnit;
import manifold.science.api.UnitCache;
import manifold.science.util.Rational;


import static manifold.science.MetricScaleUnit.*;
import static manifold.science.util.CoercionConstants.r;

/**
 * The kilogram is the SI unit of mass. All instances of {@code MassUnit} are a factor of one kilogram.
 * <p/>
 * <i>Since the revision of the SI on 20 May 2019, we can now compare the gravitational force on an object with an
 * electromagnetic force using a Kibble balance. This allows the kilogram to be defined in term of a fixed numerical
 * value of the Planck constant, a constant which will not change over time.</i>
 * (ref. <a href="https://www.npl.co.uk/si-units/kilogram">npl.co.uk</a>)
 */
public final class MassUnit extends AbstractPrimaryUnit<Mass, MassUnit>
{
  private static final UnitCache<MassUnit> CACHE = new UnitCache<>();

  /**
   * Get or create a unit based on the {@code kilogramFactor}, which is a factor of the mass of one kilogram. The
   * specified unit is cached and will be returned for subsequent calls to this method if the {@code kilogramFactor}
   * matches.
   * <p/>
   * @param kilogramFactor A factor of the the mass of one kilogram.
   * @param name The standard full name of the unit e.g., "Pound".
   * @param symbol The standard symbol used for the unit e.g., "lb".
   * @return The specified unit.
   */
  public static MassUnit get( Rational kilogramFactor, String name, String symbol )
  {
    return CACHE.get( new MassUnit( kilogramFactor, name, symbol ) );
  }

  public static final MassUnit AtomicMass = get( 1.6605402e-27r, "AMU", "amu" );
  public static final MassUnit Nano = get( 1 p, "Nanogram", "µg" );
  public static final MassUnit Micro = get( 1 n, "Microgram", "µg" );
  public static final MassUnit Milli = get( 1 u, "Milligram", "mg" );
  public static final MassUnit Gram = get( 1 m, "Gram", "g" );
  public static final MassUnit Kilogram = get( 1 r, "Kilogram", "kg" );
  public static final MassUnit Tonne = get( 1 k, "Metric Ton", "tonne" );
  public static final MassUnit Carat = get( 0.0002r, "Carat", "ct" );
  public static final MassUnit Dram = get( 0.001771845195312r, "Dram", "dr" );
  public static final MassUnit Grain = get( 6.47989e-5r, "Grain", "gr" );
  public static final MassUnit Newton = get( 0.101971621r, "Newton", "N" );
  public static final MassUnit Ounce = get( 0.0283495r, "Ounce", "oz" );
  public static final MassUnit TroyOunce = get( 0.0311035r, "Troy Ounce", "ozt" );
  public static final MassUnit Pound = get( 0.45359237r, "Pound", "lb" );
  public static final MassUnit Slug = get( 14.593902937r, "Slug", "slug" );
  public static final MassUnit Stone = get( 6.35029r, "Stone", "st" );
  public static final MassUnit Ton = get( 907.185r, "Ton (US, short)", "sht" );
  public static final MassUnit TonUK = get( 1016.05r, "Ton (UK, long)", "lt" );
  public static final MassUnit Solar = get( 1.9889200011445836e30r, "Solar Masses", "M☉" );

  public static final MassUnit BASE = Kilogram;

  private MassUnit( Rational kilogramFactor, String name, String symbol )
  {
    super( kilogramFactor, name, symbol );
  }

  @Override
  public Mass makeDimension( Number amount )
  {
    return new Mass( Rational.get( amount ), this );
  }
  
  public MomentumUnit times( VelocityUnit velocity )
  {
    return MomentumUnit.get( this, velocity );
  }

  public ForceUnit times( AccelerationUnit acc )
  {
    return ForceUnit.get( this, acc );
  }

  public PressureUnit div( AreaUnit area )
  {
    return PressureUnit.get( this, area );
  }

  public AreaUnit div( PressureUnit pressure )
  {
    return pressure.getAreaUnit();
  }

  public DensityUnit div( VolumeUnit volume )
  {
    return DensityUnit.get( this, volume );
  }

  public VolumeUnit div( DensityUnit d )
  {
    return d.getVolumeUnit();
  }
}
