package manifold.science;

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;

public final class Volume extends AbstractMeasure<VolumeUnit, Volume>
{
  public Volume( Rational value, VolumeUnit unit, VolumeUnit displayUnit ) {
    super( value, unit, displayUnit, VolumeUnit.BASE );
  }
  public Volume( Rational value, VolumeUnit unit ) {
    this( value, unit, unit );
  }

  @Override
  public Volume make( Rational value, VolumeUnit unit, VolumeUnit displayUnit )
  {
    return new Volume( value, unit, displayUnit );
  }

  @Override
  public Volume make( Rational value, VolumeUnit unit )
  {
    return new Volume( value, unit );
  }

  public Area divide( Length t ) {
    return new Area( toBaseNumber() / t.toBaseNumber(), AreaUnit.BASE, AreaUnit.get( t.getUnit() ) );
  }
  
  public Length divide( Area area ) {
    return new Length( toBaseNumber() / area.toBaseNumber(), LengthUnit.BASE, area.getUnit().getWidthUnit() );
  }
  
  public Mass multiply( Density density ) {
    return new Mass( toBaseNumber() * density.toBaseNumber(), MassUnit.BASE, density.getUnit().getMassUnit() );
  }
}
