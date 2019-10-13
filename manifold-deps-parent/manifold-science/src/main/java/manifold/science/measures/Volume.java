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

public final class Volume extends AbstractMeasure<VolumeUnit, Volume>
{
  public Volume( Rational value, VolumeUnit unit, VolumeUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Volume( Rational value, VolumeUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public VolumeUnit getBaseUnit()
  {
    return VolumeUnit.BASE;
  }

  @Override
  public Volume make( Rational value, VolumeUnit unit, VolumeUnit displayUnit )
  {
    return new Volume( value, unit, displayUnit );
  }

  @Override
  public Volume make( Rational value, VolumeUnit unit )
  {
    return new Volume( value, unit );
  }

  public Area div( Length t )
  {
    return new Area( toBaseNumber() / t.toBaseNumber(), AreaUnit.BASE, AreaUnit.get( t.getDisplayUnit() ) );
  }

  public Length div( Area area )
  {
    return new Length( toBaseNumber() / area.toBaseNumber(), LengthUnit.BASE, area.getDisplayUnit().getWidthUnit() );
  }

  public Mass times( Density density )
  {
    return new Mass( toBaseNumber() * density.toBaseNumber(), MassUnit.BASE, density.getDisplayUnit().getMassUnit() );
  }
}
