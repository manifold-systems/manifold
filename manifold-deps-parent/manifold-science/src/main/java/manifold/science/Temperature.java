package manifold.science;

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;

public final class Temperature extends AbstractMeasure<TemperatureUnit, Temperature>
{
  public Temperature( Rational value, TemperatureUnit unit, TemperatureUnit displayUnit ) {
    super( value, unit, displayUnit, TemperatureUnit.Kelvin );
  }
  public Temperature( Rational value, TemperatureUnit unit ) {
    this( value, unit, unit );
  }

  @Override
  public Temperature make( Rational value, TemperatureUnit unit, TemperatureUnit displayUnit )
  {
    return new Temperature( value, unit, displayUnit );
  }
  @Override
  public Temperature make( Rational value, TemperatureUnit unit )
  {
    return new Temperature( value, unit );
  }

  public Energy multiply( HeatCapacity c ) {
    return new Energy( toBaseNumber() * c.toBaseNumber(), EnergyUnit.BASE, c.getUnit().getEnergyUnit() );
  }  
}
