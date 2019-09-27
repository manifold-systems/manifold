package manifold.science;

import java.util.function.Function;
import manifold.science.api.IUnit;
import manifold.science.util.Rational;


import static manifold.science.MetricScaleUnit.r;

public enum TemperatureUnit implements IUnit<Temperature, TemperatureUnit>
{
  Kelvin( degK -> degK, degK -> degK, "Kelvin", "K" ),
  Celcius( degC -> degC + "273.15"r, degK -> degK - "273.15"r, "Celcius", "°C" ),
  Fahrenheit( degF -> (degF + "459.67"r) * "5/9"r, degK -> degK * "9/5"r - "459.67"r, "Fahrenheit", "°F" ),
  Rankine( degR -> degR * "5/9"r, degK -> degK * "9/5"r, "Rankine", "°R" ),
  Delisle( De -> "373.15"r - De * "2/3"r, degK -> ("373.15"r - degK) * "3/2"r, "Delisle", "°De" ),
  Newton( degN -> degN * "100/33"r + "273.15"r, degK -> (degK - "273.15"r) * "33/100"r, "Newton", "°N" );

  private final Function<Rational, Rational> _toK;
  private final Function<Rational, Rational> _fromK;
  private final String _name;
  private final String _symbol;

  public static final TemperatureUnit BASE = Kelvin;

  TemperatureUnit( Function<Rational, Rational> toK, Function<Rational, Rational> fromK, String name, String symbol ) {
    _toK = toK;
    _fromK = fromK;
    _name = name;
    _symbol = symbol;
  }

  @Override
  public Temperature makeDimension( Number amount )
  {
    return new Temperature( Rational.get( amount ), this );
  }

  public String getUnitName() {
    return _name;
  }

  public String getUnitSymbol() {
    return _symbol;
  }

  public Rational toBaseUnits( Rational myUnits ) {
    return _toK.apply( myUnits );
  }

  public Rational toNumber() {
    return _toK.apply( 1r );
  }

  public Rational from( Temperature t ) {
    return _fromK.apply( t.toBaseNumber() );
  }

  public EnergyUnit multiply( HeatCapacityUnit c ) {
    return c.getEnergyUnit();
  }
}
