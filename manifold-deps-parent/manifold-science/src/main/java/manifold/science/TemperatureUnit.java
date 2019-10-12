package manifold.science;

import java.util.Objects;
import java.util.function.Function;
import manifold.science.api.Unit;
import manifold.science.api.UnitCache;
import manifold.science.util.Rational;


import static manifold.science.util.CoercionConstants.r;

/**
 * The kelvin is the SI unit of thermodynamic temperature. All instances of {@code TemperatureUnit} convert to and from
 * degrees Kelvin.
 * <p/>
 * <i>Since May 2019, the kelvin and degree Celsius have been defined by taking a fixed numerical value of the Boltzmann
 * constant, k or kB. This change in definition acknowledges that temperature is fundamentally a measure of the average
 * energy of molecular motion.</i>
 * (ref. <a href="https://www.npl.co.uk/si-units/kelvin">npl.co.uk</a>)
 */
public final class TemperatureUnit implements Unit<Temperature, TemperatureUnit>
{
  private static final UnitCache<TemperatureUnit> CACHE = new UnitCache<>();

  /**
   * Get or create a unit using the {@code toK} and {@code fromK} conversion functions. The specified unit is cached
   * based on the {@code name} and {@code symbol} and will be returned for subsequent calls to this method if
   * {@code name} and {@code symbol} match.
   * <p/>
   * @param toK A function to convert the named unit to degrees Kelvin.
   * @param fromK A function to convert from degrees Kelvin to degrees of the named unit.
   * @param name The standard full name of the unit e.g., "Celsius".
   * @param symbol The standard symbol used for the unit e.g., "°C".
   * @return The specified unit.
   */
  public static TemperatureUnit get( Function<Rational, Rational> toK, Function<Rational, Rational> fromK, String name, String symbol )
  {
    return CACHE.get( new TemperatureUnit( toK, fromK, name, symbol ) );
  }

  public static final TemperatureUnit Kelvin = get( degK -> degK, degK -> degK, "Kelvin", "K" );
  public static final TemperatureUnit Celsius = get( degC -> degC + 273.15r, degK -> degK - 273.15r, "Celsius", "°C" );
  public static final TemperatureUnit Fahrenheit = get( degF -> (degF + 459.67r ) * "5/9"r, degK -> degK * "9/5"r - 459.67r, "Fahrenheit", "°F" );

  public static final TemperatureUnit Rankine = get( degR -> degR * "5/9"r, degK -> degK * "9/5"r, "Rankine", "°R" );

  public static final TemperatureUnit Delisle = get( De -> 373.15r - De * "2/3"r, degK -> (373.15r - degK) * "3/2"r, "Delisle", "°De" );

  public static final TemperatureUnit Newton = get( degN -> degN * "100/33"r + 273.15r, degK -> (degK - 273.15r) * "33/100"r, "Newton", "°N" );

  private final Function<Rational, Rational> _toK;
  private final Function<Rational, Rational> _fromK;
  private final String _name;
  private final String _symbol;

  public static final TemperatureUnit BASE = Kelvin;

  public TemperatureUnit( Function<Rational, Rational> toK, Function<Rational, Rational> fromK, String name, String symbol )
  {
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

  public String getName()
  {
    return _name;
  }

  public String getSymbol()
  {
    return _symbol;
  }

  public Rational toBaseUnits( Rational myUnits )
  {
    return _toK.apply( myUnits );
  }

  public Rational toNumber()
  {
    return _toK.apply( Rational.ONE );
  }

  public Rational from( Temperature t )
  {
    return _fromK.apply( t.toBaseNumber() );
  }

  public EnergyUnit times( HeatCapacityUnit c )
  {
    return c.getEnergyUnit();
  }

  @Override
  public boolean equals( Object o )
  {
    if( this == o )
    {
      return true;
    }
    if( !(o instanceof TemperatureUnit) )
    {
      return false;
    }
    TemperatureUnit that = (TemperatureUnit)o;
    return _name.equalsIgnoreCase( that._name ) &&
           _symbol.equalsIgnoreCase( that._symbol );
  }

  @Override
  public int hashCode()
  {
    return Objects.hash( _name, _symbol );
  }
}
