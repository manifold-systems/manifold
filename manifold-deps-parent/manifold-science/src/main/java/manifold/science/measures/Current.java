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


import static manifold.science.measures.ChargeUnit.Coulomb;

public final class Current extends AbstractMeasure<CurrentUnit, Current>
{
  public Current( Rational value, CurrentUnit unit, CurrentUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Current( Rational value, CurrentUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public CurrentUnit getBaseUnit()
  {
    return CurrentUnit.BASE;
  }

  @Override
  public Current make( Rational value, CurrentUnit unit, CurrentUnit displayUnit )
  {
    return new Current( value, unit, displayUnit );
  }

  @Override
  public Current make( Rational value, CurrentUnit unit )
  {
    return new Current( value, unit );
  }

  public Charge times( Time time )
  {
    return new Charge( toBaseNumber() * time.toBaseNumber(), Coulomb );
  }

  public Conductance div( Potential p )
  {
    return new Conductance( toBaseNumber() / p.toBaseNumber(), ConductanceUnit.BASE, ConductanceUnit.get( getDisplayUnit(), p.getDisplayUnit() ) );
  }

  public Potential div( Conductance c )
  {
    return new Potential( toBaseNumber() / c.toBaseNumber(), PotentialUnit.BASE, c.getDisplayUnit().getPotentialUnit() );
  }
}
