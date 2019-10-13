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

public final class Frequency extends AbstractMeasure<FrequencyUnit, Frequency>
{
  public Frequency( Rational value, FrequencyUnit unit, FrequencyUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Frequency( Rational value, FrequencyUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public FrequencyUnit getBaseUnit()
  {
    return FrequencyUnit.BASE;
  }

  @Override
  public Frequency make( Rational value, FrequencyUnit unit, FrequencyUnit displayUnit )
  {
    return new Frequency( value, unit, displayUnit );
  }

  @Override
  public Frequency make( Rational value, FrequencyUnit unit )
  {
    return new Frequency( value, unit );
  }

  public Angle times( Time time )
  {
    return new Angle( toBaseNumber() * time.toBaseNumber(), AngleUnit.BASE, getDisplayUnit().getAngleUnit() );
  }
}
