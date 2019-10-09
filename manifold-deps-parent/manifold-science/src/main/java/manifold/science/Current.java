package manifold.science;

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;


import static manifold.science.ChargeUnit.Coulomb;

public final class Current extends AbstractMeasure<CurrentUnit, Current>
{
  public Current( Rational value, CurrentUnit unit, CurrentUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Current( Rational value, CurrentUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public CurrentUnit getBaseUnit()
  {
    return CurrentUnit.BASE;
  }

  @Override
  public Current make( Rational value, CurrentUnit unit, CurrentUnit displayUnit )
  {
    return new Current( value, unit, displayUnit );
  }

  @Override
  public Current make( Rational value, CurrentUnit unit )
  {
    return new Current( value, unit );
  }

  public Charge times( Time time )
  {
    return new Charge( toBaseNumber() * time.toBaseNumber(), Coulomb );
  }

  public Conductance div( Potential p )
  {
    return new Conductance( toBaseNumber() / p.toBaseNumber(), ConductanceUnit.BASE, ConductanceUnit.get( getDisplayUnit(), p.getDisplayUnit() ) );
  }

  public Potential div( Conductance c )
  {
    return new Potential( toBaseNumber() / c.toBaseNumber(), PotentialUnit.BASE, c.getDisplayUnit().getPotentialUnit() );
  }
}
