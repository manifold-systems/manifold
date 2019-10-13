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

public final class Conductance extends AbstractMeasure<ConductanceUnit, Conductance>
{
  public Conductance( Rational value, ConductanceUnit unit, ConductanceUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Conductance( Rational value, ConductanceUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public ConductanceUnit getBaseUnit()
  {
    return ConductanceUnit.BASE;
  }

  @Override
  public Conductance make( Rational value, ConductanceUnit unit, ConductanceUnit displayUnit )
  {
    return new Conductance( value, unit, displayUnit );
  }

  @Override
  public Conductance make( Rational value, ConductanceUnit unit )
  {
    return new Conductance( value, unit );
  }

  public Current times( Potential potential )
  {
    return new Current( toBaseNumber() * potential.toBaseNumber(), CurrentUnit.BASE, getDisplayUnit().getCurrentUnit() );
  }
}
