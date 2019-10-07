package manifold.science;

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;

public final class Power extends AbstractMeasure<PowerUnit, Power>
{
  public Power( Rational value, PowerUnit unit, PowerUnit displayUnit ) {
    super( value, unit, displayUnit, PowerUnit.BASE );
  }
  public Power( Rational value, PowerUnit unit ) {
    this( value, unit, unit );
  }

  @Override
  public Power make( Rational value, PowerUnit unit, PowerUnit displayUnit )
  {
    return new Power( value, unit, displayUnit );
  }
  @Override
  public Power make( Rational value, PowerUnit unit )
  {
    return new Power( value, unit );
  }

  public Energy times( Time time ) {
    return new Energy( toBaseNumber() * time.toBaseNumber(), EnergyUnit.BASE, getUnit().getEnergyUnit() );
  }

  public Force div( Velocity v ) {
    return new Force( toBaseNumber() / v.toBaseNumber(), ForceUnit.BASE, getUnit().getEnergyUnit().getForceUnit() );
  }

  public Velocity div( Force force ) {
    return new Velocity( toBaseNumber() / force.toBaseNumber(), VelocityUnit.BASE, getUnit().getEnergyUnit().getForceUnit().getAccUnit().getVelocityUnit() );
  }

  public Current div( Potential potential ) {
    return new Current( toBaseNumber() / potential.toBaseNumber(), CurrentUnit.BASE, potential.getUnit().getCurrentUnit() );
  }

  public Potential div( Current current ) {
    return new Potential( toBaseNumber() / current.toBaseNumber(), PotentialUnit.BASE, PotentialUnit.get( getUnit(), current.getUnit() ) );
  }
}
