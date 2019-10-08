package manifold.science;

import manifold.science.api.AbstractQuotientUnit;
import manifold.science.api.UnitCache;
import manifold.science.util.Rational;


import static manifold.science.LengthUnit.Inch;
import static manifold.science.MassUnit.Kilogram;
import static manifold.science.MassUnit.Pound;
import static manifold.science.util.CoercionConstants.r;

public final class PressureUnit extends AbstractQuotientUnit<MassUnit, AreaUnit, Pressure, PressureUnit>
{
  private static final UnitCache<PressureUnit> CACHE = new UnitCache<>();

  public static final PressureUnit Pa = get( Kilogram, AreaUnit.BASE, 1r, "Pascal", "Pa" );
  public static final PressureUnit bar = get( Kilogram, AreaUnit.BASE, "1.0e5"r, "Bar", "bar" );
  public static final PressureUnit at = get( Kilogram, AreaUnit.BASE, "9.80665e4"r, "TechnicalAtm", "at" );
  public static final PressureUnit atm = get( Kilogram, AreaUnit.BASE, "1.01325e5"r, "StandardAtm", "atm" );
  public static final PressureUnit Torr = get( Kilogram, AreaUnit.BASE, "133.3224"r, "Torr", "Torr" );
  public static final PressureUnit psi = get( Pound, AreaUnit.get( Inch ), 1r, "Psi", "psi" );
  
  public static final PressureUnit BASE = Pa;
  
  public static PressureUnit get( MassUnit massUnit, AreaUnit areaUnit ) {
    return get( massUnit, areaUnit, null, null, null );
  }
  public static PressureUnit get( MassUnit massUnit, AreaUnit areaUnit, Rational factor, String name, String symbol ) {
    PressureUnit unit = new PressureUnit( massUnit, areaUnit, factor, name, symbol );
    return CACHE.get( unit );
  }
  
  private PressureUnit( MassUnit massUnit, AreaUnit areaUnit, Rational factor, String name, String symbol ) {
    super( massUnit, areaUnit, factor, name, symbol );
  }

  @Override
  public Pressure makeDimension( Number amount )
  {
    return new Pressure( Rational.get( amount ), this );
  }

  public MassUnit getMassUnit() {
    return getLeftUnit();
  }
  public AreaUnit getAreaUnit() {
    return getRightUnit();
  }
}