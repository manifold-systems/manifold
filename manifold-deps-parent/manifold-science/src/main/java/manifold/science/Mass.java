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

/**
 * Models a measure of mass in units of {@link MassUnit}.
 * <p/>
 * Use values of {@code Mass} directly in arithmetic expressions with other dimensions such as:
 * <pre><code>
 *   // commonly used unit abbreviations e.g., m, ft, hr, kg, etc.
 *   import static manifold.science.util.UnitConstants.*;
 *   ...
 *   Mass weight = 80.21 kg;
 *   Mass infant = 9 lb + 8.71 oz;
 *   Force f = 2120 kg * 9.807 m/s/s;
 * </code></pre>
 */
public final class Mass extends AbstractMeasure<MassUnit, Mass>
{
  public Mass( Rational value, MassUnit unit, MassUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Mass( Rational value, MassUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public MassUnit getBaseUnit()
  {
    return MassUnit.BASE;
  }

  @Override
  public Mass make( Rational value, MassUnit unit, MassUnit displayUnit )
  {
    return new Mass( value, unit, displayUnit );
  }

  @Override
  public Mass make( Rational value, MassUnit unit )
  {
    return new Mass( value, unit );
  }

  public Force times( Acceleration a )
  {
    return new Force( toBaseNumber() * a.toBaseNumber(), ForceUnit.BASE, ForceUnit.get( getDisplayUnit(), a.getDisplayUnit() ) );
  }

  public Momentum times( Velocity v )
  {
    return new Momentum( toBaseNumber() * v.toBaseNumber(), MomentumUnit.BASE, MomentumUnit.get( getDisplayUnit(), v.getDisplayUnit() ) );
  }

  public Pressure div( Area area )
  {
    return new Pressure( toBaseNumber() / area.toBaseNumber(), PressureUnit.BASE, PressureUnit.get( getDisplayUnit(), area.getDisplayUnit() ) );
  }

  public Area div( Pressure p )
  {
    return new Area( toBaseNumber() / p.toBaseNumber(), AreaUnit.BASE, p.getDisplayUnit().getAreaUnit() );
  }

  public Density div( Volume volume )
  {
    return new Density( toBaseNumber() / volume.toBaseNumber(), DensityUnit.BASE, DensityUnit.get( getDisplayUnit(), volume.getDisplayUnit() ) );
  }

  public Volume div( Density d )
  {
    return new Volume( toBaseNumber() / d.toBaseNumber(), VolumeUnit.BASE, d.getDisplayUnit().getVolumeUnit() );
  }
}
