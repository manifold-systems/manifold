package manifold.science;

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;

public final class Pressure extends AbstractMeasure<PressureUnit, Pressure>
{
  public Pressure( Rational value, PressureUnit unit, PressureUnit displayUnit ) {
    super( value, unit, displayUnit, PressureUnit.BASE );
  }
  public Pressure( Rational value, PressureUnit unit ) {
    this( value, unit, unit );
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

  public Mass times( Area w ) {
    return new Mass( toBaseNumber() * w.toBaseNumber(), MassUnit.BASE, getUnit().getMassUnit() );
  }
}
