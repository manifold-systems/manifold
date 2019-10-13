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

public final class Capacitance extends AbstractMeasure<CapacitanceUnit, Capacitance>
{
  public Capacitance( Rational value, CapacitanceUnit unit, CapacitanceUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Capacitance( Rational value, CapacitanceUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public CapacitanceUnit getBaseUnit()
  {
    return CapacitanceUnit.BASE;
  }

  @Override
  public Capacitance make( Rational value, CapacitanceUnit unit, CapacitanceUnit displayUnit )
  {
    return new Capacitance( value, unit, displayUnit );
  }

  @Override
  public Capacitance make( Rational value, CapacitanceUnit unit )
  {
    return new Capacitance( value, unit );
  }

  public Charge times( Potential potential )
  {
    return new Charge( toBaseNumber() * potential.toBaseNumber(), ChargeUnit.BASE, getDisplayUnit().getChargeUnit() );
  }
}
