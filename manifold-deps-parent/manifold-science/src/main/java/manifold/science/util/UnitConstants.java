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

package manifold.science.util;

import manifold.science.AngleUnit;
import manifold.science.ChargeUnit;
import manifold.science.CurrentUnit;
import manifold.science.EnergyUnit;
import manifold.science.ForceUnit;
import manifold.science.FrequencyUnit;
import manifold.science.HeatCapacityUnit;
import manifold.science.LengthUnit;
import manifold.science.MassUnit;
import manifold.science.MomentumUnit;
import manifold.science.PowerUnit;
import manifold.science.TemperatureUnit;
import manifold.science.TimeUnit;
import manifold.science.VelocityUnit;
import manifold.science.VolumeUnit;
import manifold.science.api.Dimension;

/**
 * A collection of commonly used SI units specified as standard abbreviations.
 * <p/>
 * Import constants of this class like this:
 * <pre><code>
 *   import static manifold.science.util.UnitConstants.m;
 * </code></pre>
 * Then use them to conveniently express {@link Dimension} values like this:
 * <pre><code>
 *   Length distance = 90 mph * 25 min;
 * </code></pre>
 * Note unlike floating point literals, these expressions retain the precision of the literal decimal values.
 */
public interface UnitConstants
{
  LengthUnit mum = LengthUnit.Micro;
  LengthUnit mm = LengthUnit.Milli;
  LengthUnit cm = LengthUnit.Centi;
  LengthUnit m = LengthUnit.Meter;
  LengthUnit km = LengthUnit.Kilometer;
  LengthUnit in = LengthUnit.Inch;
  LengthUnit ft = LengthUnit.Foot;
  LengthUnit yd = LengthUnit.Yard;
  LengthUnit mi = LengthUnit.Mile;

  TimeUnit ns = TimeUnit.Nano;
  TimeUnit mus = TimeUnit.Micro;
  TimeUnit ms = TimeUnit.Milli;
  TimeUnit s = TimeUnit.Second;
  TimeUnit min = TimeUnit.Minute;
  TimeUnit hr = TimeUnit.Hour;
  TimeUnit day = TimeUnit.Day;
  TimeUnit wk = TimeUnit.Week;
  TimeUnit mo = TimeUnit.Month;
  TimeUnit yr = TimeUnit.Year;
  TimeUnit tmo = TimeUnit.TrMonth;
  TimeUnit tyr = TimeUnit.TrYear;

  VelocityUnit mph = mi/hr;

  MassUnit amu = MassUnit.AtomicMass;
  MassUnit mug = MassUnit.Micro;
  MassUnit mg = MassUnit.Milli;
  MassUnit g = MassUnit.Gram;
  MassUnit kg = MassUnit.Kilogram;
  MassUnit ct = MassUnit.Carat;
  MassUnit dr = MassUnit.Dram;
  MassUnit gr = MassUnit.Grain;
  MassUnit Nt = MassUnit.Newton;
  MassUnit oz = MassUnit.Ounce;
  MassUnit ozt = MassUnit.TroyOunce;
  MassUnit lb = MassUnit.Pound;
  MassUnit st = MassUnit.Stone;
  MassUnit sht = MassUnit.Ton;
  MassUnit lt = MassUnit.TonUK;
  MassUnit tonne = MassUnit.Tonne;
  MassUnit Mo = MassUnit.Solar;

  VolumeUnit L = VolumeUnit.LITER;
  VolumeUnit mL = VolumeUnit.MILLI_LITER;
  VolumeUnit fl_oz = VolumeUnit.FLUID_OZ;
  VolumeUnit gal = VolumeUnit.GALLON;
  VolumeUnit qt = VolumeUnit.QUART;
  VolumeUnit pt = VolumeUnit.PINT;
  VolumeUnit cup = VolumeUnit.CUP;
  VolumeUnit tbsp = VolumeUnit.TABLE_SPOON;
  VolumeUnit tsp = VolumeUnit.TEA_SPOON;

  AngleUnit cyc = AngleUnit.Turn;
  AngleUnit rad = AngleUnit.Radian;
  AngleUnit mrad = AngleUnit.Milli;
  AngleUnit nrad = AngleUnit.Nano;
  AngleUnit arcsec = AngleUnit.ArcSecond;
  AngleUnit mas = AngleUnit.MilliArcSecond;
  AngleUnit grad = AngleUnit.Gradian;
  AngleUnit quad = AngleUnit.Quadrant;
  AngleUnit moa = AngleUnit.MOA;
  AngleUnit deg = AngleUnit.Degree;

  TemperatureUnit dK = TemperatureUnit.Kelvin;
  TemperatureUnit dC = TemperatureUnit.Celcius;
  TemperatureUnit dF = TemperatureUnit.Fahrenheit;

  MomentumUnit Ns = kg m/s;

  ForceUnit N = kg m/s/s;
  ForceUnit dyn = g cm/s/s;

  EnergyUnit joule = N m;
  EnergyUnit J = joule;
  EnergyUnit erg = dyn cm;
  EnergyUnit kcal = EnergyUnit.kcal;

  PowerUnit watt = J / s;
  PowerUnit W = watt;

  HeatCapacityUnit C = J / dK;

  FrequencyUnit Hz = cyc / s;
  FrequencyUnit kHz = cyc/ms;
  FrequencyUnit MHz = cyc/mus;
  FrequencyUnit GHz = cyc/ns;
  FrequencyUnit rpm = cyc/min;

  ChargeUnit coulomb = ChargeUnit.Coulomb;
  CurrentUnit amp = coulomb / s;
}

