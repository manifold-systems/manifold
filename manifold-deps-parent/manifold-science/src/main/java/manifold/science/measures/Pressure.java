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

public final class Pressure extends AbstractMeasure<PressureUnit, Pressure>
{
  public Pressure( Rational value, PressureUnit unit, PressureUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Pressure( Rational value, PressureUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public PressureUnit getBaseUnit()
  {
    return PressureUnit.BASE;
  }

  @Override
  public Pressure make( Rational value, PressureUnit unit, PressureUnit displayUnit )
  {
    return new Pressure( value, unit, displayUnit );
  }

  @Override
  public Pressure make( Rational value, PressureUnit unit )
  {
    return new Pressure( value, unit );
  }

  public Mass times( Area w )
  {
    return new Mass( toBaseNumber() * w.toBaseNumber(), MassUnit.BASE, getDisplayUnit().getMassUnit() );
  }
}
