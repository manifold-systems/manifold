package manifold.science;

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;

public final class Potential extends AbstractMeasure<PotentialUnit, Potential>
{
  public Potential( Rational value, PotentialUnit unit, PotentialUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Potential( Rational value, PotentialUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public PotentialUnit getBaseUnit()
  {
    return PotentialUnit.BASE;
  }

  @Override
  public Potential make( Rational value, PotentialUnit unit, PotentialUnit displayUnit )
  {
    return new Potential( value, unit, displayUnit );
  }

  @Override
  public Potential make( Rational value, PotentialUnit unit )
  {
    return new Potential( value, unit );
  }

  public Power times( Current current )
  {
    return new Power( toBaseNumber() * current.toBaseNumber(), PowerUnit.BASE, getDisplayUnit().getPowerUnit() );
  }

  public Resistance div( Current i )
  {
    return new Resistance( toBaseNumber() / i.toBaseNumber(), ResistanceUnit.BASE, ResistanceUnit.get( getDisplayUnit(), i.getDisplayUnit() ) );
  }

  public Current div( Resistance r )
  {
    return new Current( toBaseNumber() / r.toBaseNumber(), CurrentUnit.BASE, r.getDisplayUnit().getCurrentUnit() );
  }
}
