package manifold.science;

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;

public final class Inductance extends AbstractMeasure<InductanceUnit, Inductance>
{
  public Inductance( Rational value, InductanceUnit unit, InductanceUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Inductance( Rational value, InductanceUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public InductanceUnit getBaseUnit()
  {
    return InductanceUnit.BASE;
  }

  @Override
  public Inductance make( Rational value, InductanceUnit unit, InductanceUnit displayUnit )
  {
    return new Inductance( value, unit, displayUnit );
  }

  @Override
  public Inductance make( Rational value, InductanceUnit unit )
  {
    return new Inductance( value, unit );
  }

  public Resistance div( Time time )
  {
    return new Resistance( toBaseNumber() / time.toBaseNumber(), ResistanceUnit.BASE, getDisplayUnit().getResistanceUnit() );
  }

  public Time div( Resistance resistance )
  {
    return new Time( toBaseNumber() / resistance.toBaseNumber(), TimeUnit.BASE, getDisplayUnit().getTimeUnit() );
  }
}
