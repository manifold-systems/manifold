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

/**
 * Models a measure of length in units of {@link LengthUnit}.
 * <p/>
 * Use values of {@code Length} directly in arithmetic expressions with other dimensions such as:
 * <pre><code>
 *   // commonly used unit abbreviations e.g., m, ft, hr, mph, etc.
 *   import static manifold.science.util.UnitConstants.*;
 *   ...
 *   Length l = 5m; // 5 meters
 *   Length height = 5 ft + 9.5 in;
 *   Area room = 20 ft * 15.5 ft;
 *   Length distance = 80 mph * 2.3 hr;
 * </code></pre>
 */
public final class Length extends AbstractMeasure<LengthUnit, Length>
{
  public Length( Rational value, LengthUnit unit, LengthUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Length( Rational value, LengthUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public LengthUnit getBaseUnit()
  {
    return LengthUnit.BASE;
  }

  @Override
  public Length make( Rational value, LengthUnit unit, LengthUnit displayUnit )
  {
    return new Length( value, unit, displayUnit );
  }

  @Override
  public Length make( Rational value, LengthUnit unit )
  {
    return new Length( value, unit, unit );
  }

  public Velocity div( Time t )
  {
    return new Velocity( toBaseNumber() / t.toBaseNumber(), VelocityUnit.BASE, VelocityUnit.get( getDisplayUnit(), t.getDisplayUnit() ) );
  }

  public Time div( Velocity v )
  {
    return new Time( toBaseNumber() / v.toBaseNumber(), TimeUnit.BASE, v.getDisplayUnit().getTimeUnit() );
  }

  public Area times( Length len )
  {
    return new Area( toBaseNumber() * len.toBaseNumber(), AreaUnit.BASE, AreaUnit.get( getDisplayUnit(), len.getDisplayUnit() ) );
  }

  public Volume times( Area area )
  {
    return new Volume( toBaseNumber() * area.toBaseNumber(), VolumeUnit.BASE, VolumeUnit.get( getDisplayUnit(), area.getDisplayUnit() ) );
  }

  public Energy times( Force force )
  {
    return new Energy( toBaseNumber() * force.toBaseNumber(), EnergyUnit.BASE, EnergyUnit.get( force.getDisplayUnit(), getDisplayUnit() ) );
  }
}
