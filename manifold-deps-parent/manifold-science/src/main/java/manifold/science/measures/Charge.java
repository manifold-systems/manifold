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

public final class Charge extends AbstractMeasure<ChargeUnit, Charge>
{
  public Charge( Rational value, ChargeUnit unit, ChargeUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Charge( Rational value, ChargeUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public ChargeUnit getBaseUnit()
  {
    return ChargeUnit.BASE;
  }

  @Override
  public Charge make( Rational value, ChargeUnit unit, ChargeUnit displayUnit )
  {
    return new Charge( value, unit, displayUnit );
  }

  @Override
  public Charge make( Rational value, ChargeUnit unit )
  {
    return new Charge( value, unit );
  }

  public Current div( Time time )
  {
    return new Current( toBaseNumber() / time.toBaseNumber(), CurrentUnit.BASE, CurrentUnit.get( getDisplayUnit(), time.getDisplayUnit() ) );
  }

  public Time div( Current i )
  {
    return new Time( toBaseNumber() / i.toBaseNumber(), TimeUnit.BASE, i.getDisplayUnit().getTimeUnit() );
  }

  public Capacitance div( Potential p )
  {
    return new Capacitance( toBaseNumber() / p.toBaseNumber(), CapacitanceUnit.BASE, CapacitanceUnit.get( getDisplayUnit(), p.getDisplayUnit() ) );
  }

  public Potential div( Capacitance cap )
  {
    return new Potential( toBaseNumber() / cap.toBaseNumber(), PotentialUnit.BASE, cap.getDisplayUnit().getPotentialUnit() );
  }
}
