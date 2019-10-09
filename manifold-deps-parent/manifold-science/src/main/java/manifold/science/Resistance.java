package manifold.science;

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;

public final class Resistance extends AbstractMeasure<ResistanceUnit, Resistance>
{
  public Resistance( Rational value, ResistanceUnit unit, ResistanceUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Resistance( Rational value, ResistanceUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public ResistanceUnit getBaseUnit()
  {
    return ResistanceUnit.BASE;
  }

  @Override
  public Resistance make( Rational value, ResistanceUnit unit, ResistanceUnit displayUnit )
  {
    return new Resistance( value, unit, displayUnit );
  }

  @Override
  public Resistance make( Rational value, ResistanceUnit unit )
  {
    return new Resistance( value, unit );
  }

  public Potential times( Current current )
  {
    return new Potential( toBaseNumber() * current.toBaseNumber(), PotentialUnit.BASE, getDisplayUnit().getPotentialUnit() );
  }

  public Inductance times( Time time )
  {
    return new Inductance( toBaseNumber() * time.toBaseNumber(), InductanceUnit.BASE, getDisplayUnit() * time.getDisplayUnit() );
  }
}
