package manifold.science;

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;

public final class Conductance extends AbstractMeasure<ConductanceUnit, Conductance>
{
  public Conductance( Rational value, ConductanceUnit unit, ConductanceUnit displayUnit ) {
    super( value, unit, displayUnit, ConductanceUnit.BASE );
  }
  public Conductance( Rational value, ConductanceUnit unit ) {
    this( value, unit, unit );
  }

  @Override
  public Conductance make( Rational value, ConductanceUnit unit, ConductanceUnit displayUnit )
  {
    return new Conductance( value, unit, displayUnit );
  }
  @Override
  public Conductance make( Rational value, ConductanceUnit unit )
  {
    return new Conductance( value, unit );
  }

  public Current times( Potential potential ) {
    return new Current( toBaseNumber() * potential.toBaseNumber(), CurrentUnit.BASE, getUnit().getCurrentUnit() );
  }
}
