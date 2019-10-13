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

public final class Resistance extends AbstractMeasure<ResistanceUnit, Resistance>
{
  public Resistance( Rational value, ResistanceUnit unit, ResistanceUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Resistance( Rational value, ResistanceUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public ResistanceUnit getBaseUnit()
  {
    return ResistanceUnit.BASE;
  }

  @Override
  public Resistance make( Rational value, ResistanceUnit unit, ResistanceUnit displayUnit )
  {
    return new Resistance( value, unit, displayUnit );
  }

  @Override
  public Resistance make( Rational value, ResistanceUnit unit )
  {
    return new Resistance( value, unit );
  }

  public Potential times( Current current )
  {
    return new Potential( toBaseNumber() * current.toBaseNumber(), PotentialUnit.BASE, getDisplayUnit().getPotentialUnit() );
  }

  public Inductance times( Time time )
  {
    return new Inductance( toBaseNumber() * time.toBaseNumber(), InductanceUnit.BASE, getDisplayUnit() * time.getDisplayUnit() );
  }
}
