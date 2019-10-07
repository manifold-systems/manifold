package manifold.science;

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;

public final class MagneticFluxDensity extends AbstractMeasure<MagneticFluxDensityUnit, MagneticFluxDensity>
{
  public MagneticFluxDensity( Rational value, MagneticFluxDensityUnit unit, MagneticFluxDensityUnit displayUnit ) {
    super( value, unit, displayUnit, MagneticFluxDensityUnit.BASE );
  }
  public MagneticFluxDensity( Rational value, MagneticFluxDensityUnit unit ) {
    this( value, unit, unit );
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

  public MagneticFlux times( Area area ) {
    return new MagneticFlux( toBaseNumber() * area.toBaseNumber(), MagneticFluxUnit.BASE, getUnit().getMagneticFluxUnit() );
  }
}
