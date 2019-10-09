package manifold.science;

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;

public final class Momentum extends AbstractMeasure<MomentumUnit, Momentum>
{
  public Momentum( Rational value, MomentumUnit unit, MomentumUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Momentum( Rational value, MomentumUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public MomentumUnit getBaseUnit()
  {
    return MomentumUnit.BASE;
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

  public Energy times( Velocity v )
  {
    return new Energy( toBaseNumber() * v.toBaseNumber(), EnergyUnit.BASE, getDisplayUnit() * v.getDisplayUnit() );
  }

  public Velocity div( Mass mass )
  {
    return new Velocity( toBaseNumber() / mass.toBaseNumber(), VelocityUnit.BASE, getDisplayUnit().getVelocityUnit() );
  }

  public Mass div( Velocity v )
  {
    return new Mass( toBaseNumber() / v.toBaseNumber(), MassUnit.BASE, getDisplayUnit().getMassUnit() );
  }
}