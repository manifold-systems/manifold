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

public final class MagneticFluxDensity extends AbstractMeasure<MagneticFluxDensityUnit, MagneticFluxDensity>
{
  public MagneticFluxDensity( Rational value, MagneticFluxDensityUnit unit, MagneticFluxDensityUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public MagneticFluxDensity( Rational value, MagneticFluxDensityUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public MagneticFluxDensityUnit getBaseUnit()
  {
    return MagneticFluxDensityUnit.BASE;
  }

  @Override
  public MagneticFluxDensity make( Rational value, MagneticFluxDensityUnit unit, MagneticFluxDensityUnit displayUnit )
  {
    return new MagneticFluxDensity( value, unit, displayUnit );
  }

  @Override
  public MagneticFluxDensity make( Rational value, MagneticFluxDensityUnit unit )
  {
    return new MagneticFluxDensity( value, unit );
  }

  public MagneticFlux times( Area area )
  {
    return new MagneticFlux( toBaseNumber() * area.toBaseNumber(), MagneticFluxUnit.BASE, getDisplayUnit().getMagneticFluxUnit() );
  }
}
