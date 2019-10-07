package manifold.science;

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;

public final class Momentum extends AbstractMeasure<MomentumUnit, Momentum>
{
  public Momentum( Rational value, MomentumUnit unit, MomentumUnit displayUnit ) {
    super( value, unit, displayUnit, MomentumUnit.BASE );
  }
  public Momentum( Rational value, MomentumUnit unit ) {
    this( value, unit, unit );
  }

  @Override
  public Momentum make( Rational value, MomentumUnit unit, MomentumUnit displayUnit )
  {
    return new Momentum( value, unit, displayUnit );
  }
  @Override
  public Momentum make( Rational value, MomentumUnit unit )
  {
    return new Momentum( value, unit );
  }

  public Energy times( Velocity v ) {
    return new Energy( toBaseNumber() * v.toBaseNumber(), EnergyUnit.BASE, getUnit() * v.getUnit() );
  }
  
  public Velocity div( Mass mass ) {
    return new Velocity( toBaseNumber() / mass.toBaseNumber(), VelocityUnit.BASE, getUnit().getVelocityUnit() );
  }
  public Mass div( Velocity v ) {
    return new Mass( toBaseNumber() / v.toBaseNumber(), MassUnit.BASE, getUnit().getMassUnit() );
  }
}