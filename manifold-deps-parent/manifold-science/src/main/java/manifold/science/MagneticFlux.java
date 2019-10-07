package manifold.science;

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;

public final class MagneticFlux extends AbstractMeasure<MagneticFluxUnit, MagneticFlux>
{
  public MagneticFlux( Rational value, MagneticFluxUnit unit, MagneticFluxUnit displayUnit ) {
    super( value, unit, displayUnit, MagneticFluxUnit.BASE );
  }
  public MagneticFlux( Rational value, MagneticFluxUnit unit ) {
    this( value, unit, unit );
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

  public Energy times( Current current ) {
    return new Energy( toBaseNumber() * current.toBaseNumber(), EnergyUnit.BASE, getUnit().getEnergyUnit() );
  }

  public MagneticFluxDensity div( Area area ) {
    return new MagneticFluxDensity( toBaseNumber() / area.toBaseNumber(),
      MagneticFluxDensityUnit.BASE, MagneticFluxDensityUnit.get( getUnit(), area.getUnit() ) );
  }

  public Area div( MagneticFluxDensity mf ) {
    return new Area( toBaseNumber() / mf.toBaseNumber(), AreaUnit.BASE, mf.getUnit().getAreaUnit() );
  }
}
