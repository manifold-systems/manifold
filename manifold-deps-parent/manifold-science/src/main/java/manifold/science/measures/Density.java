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


import static manifold.science.measures.MassUnit.Kilogram;

public final class Density extends AbstractMeasure<DensityUnit, Density>
{
  public Density( Rational value, DensityUnit unit, DensityUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Density( Rational value, DensityUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public DensityUnit getBaseUnit()
  {
    return DensityUnit.BASE;
  }

  @Override
  public Density make( Rational value, DensityUnit unit, DensityUnit displayUnit )
  {
    return new Density( value, unit, displayUnit );
  }

  @Override
  public Density make( Rational value, DensityUnit unit )
  {
    return new Density( value, unit );
  }

  public Mass times( Volume w )
  {
    return new Mass( toBaseNumber() * w.toBaseNumber(), Kilogram, getDisplayUnit().getMassUnit() );
  }
}
