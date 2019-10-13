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

public final class MagneticFlux extends AbstractMeasure<MagneticFluxUnit, MagneticFlux>
{
  public MagneticFlux( Rational value, MagneticFluxUnit unit, MagneticFluxUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public MagneticFlux( Rational value, MagneticFluxUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public MagneticFluxUnit getBaseUnit()
  {
    return MagneticFluxUnit.BASE;
  }

  @Override
  public MagneticFlux make( Rational value, MagneticFluxUnit unit, MagneticFluxUnit displayUnit )
  {
    return new MagneticFlux( value, unit, displayUnit );
  }

  @Override
  public MagneticFlux make( Rational value, MagneticFluxUnit unit )
  {
    return new MagneticFlux( value, unit );
  }

  public Energy times( Current current )
  {
    return new Energy( toBaseNumber() * current.toBaseNumber(), EnergyUnit.BASE, getDisplayUnit().getEnergyUnit() );
  }

  public MagneticFluxDensity div( Area area )
  {
    return new MagneticFluxDensity( toBaseNumber() / area.toBaseNumber(),
      MagneticFluxDensityUnit.BASE, MagneticFluxDensityUnit.get( getDisplayUnit(), area.getDisplayUnit() ) );
  }

  public Area div( MagneticFluxDensity mf )
  {
    return new Area( toBaseNumber() / mf.toBaseNumber(), AreaUnit.BASE, mf.getDisplayUnit().getAreaUnit() );
  }
}
