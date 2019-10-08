package manifold.science;

import manifold.science.api.AbstractQuotientUnit;
import manifold.science.api.UnitCache;
import manifold.science.util.Rational;


import static manifold.science.util.CoercionConstants.r;

public final class CapacitanceUnit extends AbstractQuotientUnit<ChargeUnit, PotentialUnit, Capacitance, CapacitanceUnit>
{
  private static final UnitCache<CapacitanceUnit> CACHE = new UnitCache<>();

  public static final CapacitanceUnit F = get( ChargeUnit.BASE, PotentialUnit.BASE, 1r, "Farad", "F" );

  public static final CapacitanceUnit BASE = F;

  public static CapacitanceUnit get( ChargeUnit chargeUnit, PotentialUnit potentialUnit ) {
    return get( chargeUnit, potentialUnit, null, null, null );
  }
  public static CapacitanceUnit get( ChargeUnit chargeUnit, PotentialUnit potentialUnit, Rational factor, String name, String symbol ) {
    CapacitanceUnit unit = new CapacitanceUnit( chargeUnit, potentialUnit, factor, name, symbol );
    return CACHE.get( unit );
  }

  private CapacitanceUnit( ChargeUnit chargeUnit, PotentialUnit potentialUnit, Rational factor, String name, String symbol ) {
    super( chargeUnit, potentialUnit, factor, name, symbol );
  }

  @Override
  public Capacitance makeDimension( Number amount )
  {
    return new Capacitance( Rational.get( amount ), this );
  }

  public ChargeUnit getChargeUnit() {
    return getLeftUnit();
  }

  public PotentialUnit getPotentialUnit() {
    return getRightUnit();
  }
}
