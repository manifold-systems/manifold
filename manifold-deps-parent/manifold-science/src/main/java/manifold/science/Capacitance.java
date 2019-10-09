package manifold.science;


import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;

public final class Capacitance extends AbstractMeasure<CapacitanceUnit, Capacitance>
{
  public Capacitance( Rational value, CapacitanceUnit unit, CapacitanceUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Capacitance( Rational value, CapacitanceUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public CapacitanceUnit getBaseUnit()
  {
    return CapacitanceUnit.BASE;
  }

  @Override
  public Capacitance make( Rational value, CapacitanceUnit unit, CapacitanceUnit displayUnit )
  {
    return new Capacitance( value, unit, displayUnit );
  }

  @Override
  public Capacitance make( Rational value, CapacitanceUnit unit )
  {
    return new Capacitance( value, unit );
  }

  public Charge times( Potential potential )
  {
    return new Charge( toBaseNumber() * potential.toBaseNumber(), ChargeUnit.BASE, getDisplayUnit().getChargeUnit() );
  }
}
