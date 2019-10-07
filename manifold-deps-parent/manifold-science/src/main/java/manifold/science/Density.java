package manifold.science;

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;


import static manifold.science.MassUnit.Kilogram;

public final class Density extends AbstractMeasure<DensityUnit, Density>
{
  public Density( Rational value, DensityUnit unit, DensityUnit displayUnit ) {
    super( value, unit, displayUnit, DensityUnit.BASE );
  }
  public Density( Rational value, DensityUnit unit ) {
    this( value, unit, unit );
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

  public Mass times( Volume w ) {
    return new Mass( toBaseNumber() * w.toBaseNumber(), Kilogram, getUnit().getMassUnit() );
  }
}
