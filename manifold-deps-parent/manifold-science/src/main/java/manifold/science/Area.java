package manifold.science;

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;

public final class Area extends AbstractMeasure<AreaUnit, Area>
{
  public Area( Rational value, AreaUnit unit, AreaUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Area( Rational value, AreaUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public AreaUnit getBaseUnit()
  {
    return AreaUnit.BASE;
  }

  @Override
  public Area make( Rational value, AreaUnit unit, AreaUnit displayUnit )
  {
    return new Area( value, unit, displayUnit );
  }

  @Override
  public Area make( Rational value, AreaUnit unit )
  {
    return new Area( value, unit );
  }

  public Volume times( Length t )
  {
    return new Volume( toBaseNumber() * t.toBaseNumber(), VolumeUnit.BASE, VolumeUnit.get( t.getDisplayUnit(), getDisplayUnit() ) );
  }

  public Length div( Length t )
  {
    return new Length( toBaseNumber() / t.toBaseNumber(), LengthUnit.BASE, t.getDisplayUnit() );
  }
}
