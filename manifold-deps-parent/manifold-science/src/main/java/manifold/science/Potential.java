package manifold.science;

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;

public final class Potential extends AbstractMeasure<PotentialUnit, Potential>
{
  public Potential( Rational value, PotentialUnit unit, PotentialUnit displayUnit ) {
    super( value, unit, displayUnit, PotentialUnit.BASE );
  }
  public Potential( Rational value, PotentialUnit unit ) {
    this( value, unit, unit );
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

  public Power multiply( Current current ) {
    return new Power( toBaseNumber() * current.toBaseNumber(), PowerUnit.BASE, getUnit().getPowerUnit() );
  }

  public Resistance divide( Current i ) {
    return new Resistance( toBaseNumber() / i.toBaseNumber(), ResistanceUnit.BASE, ResistanceUnit.get( getUnit(), i.getUnit() ) );
  }
  public Current divide( Resistance r ) {
    return new Current( toBaseNumber() / r.toBaseNumber(), CurrentUnit.BASE, r.getUnit().getCurrentUnit() );
  }
}
