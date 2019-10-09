package manifold.science;

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;

public final class SolidAngle extends AbstractMeasure<SolidAngleUnit, SolidAngle>
{
  public SolidAngle( Rational value, SolidAngleUnit unit, SolidAngleUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public SolidAngle( Rational value, SolidAngleUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public SolidAngleUnit getBaseUnit()
  {
    return SolidAngleUnit.BASE;
  }

  @Override
  public SolidAngle make( Rational value, SolidAngleUnit unit, SolidAngleUnit displayUnit )
  {
    return new SolidAngle( value, unit, displayUnit );
  }

  @Override
  public SolidAngle make( Rational value, SolidAngleUnit unit )
  {
    return new SolidAngle( value, unit );
  }
}
