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

public final class Potential extends AbstractMeasure<PotentialUnit, Potential>
{
  public Potential( Rational value, PotentialUnit unit, PotentialUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Potential( Rational value, PotentialUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public PotentialUnit getBaseUnit()
  {
    return PotentialUnit.BASE;
  }

  @Override
  public Potential make( Rational value, PotentialUnit unit, PotentialUnit displayUnit )
  {
    return new Potential( value, unit, displayUnit );
  }

  @Override
  public Potential make( Rational value, PotentialUnit unit )
  {
    return new Potential( value, unit );
  }

  public Power times( Current current )
  {
    return new Power( toBaseNumber() * current.toBaseNumber(), PowerUnit.BASE, getDisplayUnit().getPowerUnit() );
  }

  public Resistance div( Current i )
  {
    return new Resistance( toBaseNumber() / i.toBaseNumber(), ResistanceUnit.BASE, ResistanceUnit.get( getDisplayUnit(), i.getDisplayUnit() ) );
  }

  public Current div( Resistance r )
  {
    return new Current( toBaseNumber() / r.toBaseNumber(), CurrentUnit.BASE, r.getDisplayUnit().getCurrentUnit() );
  }
}
