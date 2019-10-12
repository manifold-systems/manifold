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


import static manifold.science.util.CoercionConstants.r;
import static manifold.science.util.MetricFactorConstants.*;

/**
 * The meter is the SI unit of length. All instances of {@code LengthUnit} are a factor of one meter.
 * <p/>
 * <i>We measure distances by comparing objects or distances with standard lengths. Historically, we used pieces of
 * metal or the wavelength of light from standard lamps as standard lengths. Since the speed of light in vacuum, c, is
 * constant, in the SI we use the distance that light travels in a given time as our standard.</i>
 * (ref. <a href="https://www.npl.co.uk/si-units/metre">npl.co.uk</a>)
 */
public final class LengthUnit extends AbstractPrimaryUnit<Length, LengthUnit>
{
  private static final UnitCache<LengthUnit> CACHE = new UnitCache<>();

  /**
   * Get or create a unit based on the {@code meterFactor}, which is a factor of the length of one meter. The specified
   * unit is cached and will be returned for subsequent calls to this method if the {@code meterFactor} matches.
   * <p/>
   * @param meterFactor A factor of the the length of one meter.
   * @param name The standard full name of the unit e.g., "Foot".
   * @param symbol The standard symbol used for the unit e.g., "ft".
   * @return The specified unit.
   */
  public static LengthUnit get( Rational meterFactor, String name, String symbol )
  {
    return CACHE.get( new LengthUnit( meterFactor, name, symbol ) );
  }

  // SI Units
  public static final LengthUnit Femto = get(FEMTO, "Femtometer", "fm");
  public static final LengthUnit Pico = get(PICO, "Picometer", "pm");
  public static final LengthUnit Angstrom = get(1e-10r, "Angstrom", "Å");
  public static final LengthUnit Nano = get(NANO, "Nanometer", "nm");
  public static final LengthUnit Micro = get(MICRO, "Micrometre", "µm");
  public static final LengthUnit Milli = get(MILLI, "Millimeter", "mm");
  public static final LengthUnit Centi = get(CENTI, "Centimeter", "cm");
  public static final LengthUnit Deci = get(DECI, "Decimeter", "dm");
  public static final LengthUnit Meter = get(1r, "Meter", "m");
  public static final LengthUnit Kilometer = get(KILO, "Kilometer", "km");
  public static final LengthUnit Megameter = get(KILO.pow(2), "Megameter", "Mm");
  public static final LengthUnit Gigameter = get(KILO.pow(3), "Gigameter", "Gm");
  public static final LengthUnit Terameter = get(KILO.pow(4), "Terameter", "Tm");

  // US Standard
  public static final LengthUnit Caliber = get(0.000254r, "Caliber", "cal.");
  public static final LengthUnit Inch = get(0.0254r, "Inch", "in");
  public static final LengthUnit Foot = get(12 * 0.0254r, "Foot", "ft");
  public static final LengthUnit Yard = get(3 * 12 * 0.0254r, "Yard", "yd");
  public static final LengthUnit Rod = get(5.0292r, "Rod", "rd");
  public static final LengthUnit Chain = get(20.1168r, "Chain", "ch");
  public static final LengthUnit Furlong = get(201.168r, "Furlong", "fur");
  public static final LengthUnit Mile = get(1609.344r, "Mile", "mi");

  // Navigation
  public static final LengthUnit NauticalMile = get(1852r, "NauticalMile", "n.m.");

  // Very large
  public static final LengthUnit IAU = get(1.49597870e11r, "IAU-length", "au");
  public static final LengthUnit LightYear = get(9.460730473e+15r, "LightYear", "ly");

  // Very small
  public static final LengthUnit Planck = get(1.61605e-35r, "Planck-length", "ℓP");

  // Ancient
  public static final LengthUnit Cubit = get(0.4572r, "Cubit", "cbt");
  
  public static final LengthUnit BASE = Meter;

  private LengthUnit( Rational meters, String name, String symbol )
  {
    super( meters, name, symbol );
  }

  public Rational getMeters()
  {
    return toNumber();
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
