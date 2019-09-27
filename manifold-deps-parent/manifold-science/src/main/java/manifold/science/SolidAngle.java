package manifold.science;

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;

public final class SolidAngle extends AbstractMeasure<SolidAngleUnit, SolidAngle>
{
  public SolidAngle( Rational value, SolidAngleUnit unit, SolidAngleUnit displayUnit ) {
    super( value, unit, displayUnit, SolidAngleUnit.BASE );
  }
  public SolidAngle( Rational value, SolidAngleUnit unit ) {
    this( value, unit, unit );
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
