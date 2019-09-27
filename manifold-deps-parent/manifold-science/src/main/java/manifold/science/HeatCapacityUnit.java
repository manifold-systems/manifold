package manifold.science;

import manifold.science.api.AbstractQuotientUnit;
import manifold.science.api.UnitCache;
import manifold.science.util.Rational;

public final class HeatCapacityUnit extends AbstractQuotientUnit<EnergyUnit, TemperatureUnit, HeatCapacity, HeatCapacityUnit>
{
  private static final UnitCache<HeatCapacityUnit> CACHE = new UnitCache<>();

  public static final HeatCapacityUnit BASE = get( EnergyUnit.BASE, TemperatureUnit.BASE );

  public static HeatCapacityUnit get( EnergyUnit energyUnit, TemperatureUnit temperatureUnit ) {
    return get( energyUnit, temperatureUnit, null, null, null );
  }
  public static HeatCapacityUnit get( EnergyUnit energyUnit, TemperatureUnit temperatureUnit, Rational factor, String name, String symbol ) {
    HeatCapacityUnit unit = new HeatCapacityUnit( energyUnit, temperatureUnit, factor, name, symbol );
    return CACHE.get( unit );
  }
  
  private HeatCapacityUnit( EnergyUnit energyUnit, TemperatureUnit temperatureUnit, Rational factor, String name, String symbol ) {
    super( energyUnit, temperatureUnit, factor, name, symbol );
  }

  @Override
  public HeatCapacity makeDimension( Number amount )
  {
    return new HeatCapacity( Rational.get( amount ), this );
  }

  public EnergyUnit getEnergyUnit() {
    return getLeftUnit();
  }
  public TemperatureUnit getTemperatureUnit() {
    return getRightUnit();
  }
}
