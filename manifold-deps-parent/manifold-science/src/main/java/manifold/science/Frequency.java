package manifold.science;

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;

public final class Frequency extends AbstractMeasure<FrequencyUnit, Frequency>
{
  public Frequency( Rational value, FrequencyUnit unit, FrequencyUnit displayUnit ) {
    super( value, unit, displayUnit, FrequencyUnit.BASE );
  }
  public Frequency( Rational value, FrequencyUnit unit ) {
    this( value, unit, unit );
  }

  @Override
  public Frequency make( Rational value, FrequencyUnit unit, FrequencyUnit displayUnit )
  {
    return new Frequency( value, unit, displayUnit );
  }
  @Override
  public Frequency make( Rational value, FrequencyUnit unit )
  {
    return new Frequency( value, unit );
  }

  public Angle times( Time time ) {
    return new Angle( toBaseNumber() * time.toBaseNumber(), AngleUnit.BASE, getUnit().getAngleUnit() );
  }
}
