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

public final class Area extends AbstractMeasure<AreaUnit, Area>
{
  public Area( Rational value, AreaUnit unit, AreaUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Area( Rational value, AreaUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public AreaUnit getBaseUnit()
  {
    return AreaUnit.BASE;
  }

  @Override
  public Area make( Rational value, AreaUnit unit, AreaUnit displayUnit )
  {
    return new Area( value, unit, displayUnit );
  }

  @Override
  public Area make( Rational value, AreaUnit unit )
  {
    return new Area( value, unit );
  }

  public Volume times( Length t )
  {
    return new Volume( toBaseNumber() * t.toBaseNumber(), VolumeUnit.BASE, VolumeUnit.get( t.getDisplayUnit(), getDisplayUnit() ) );
  }

  public Length div( Length t )
  {
    return new Length( toBaseNumber() / t.toBaseNumber(), LengthUnit.BASE, t.getDisplayUnit() );
  }
}
