package manifold.science;

import manifold.science.api.AbstractProductUnit;
import manifold.science.api.UnitCache;
import manifold.science.util.Rational;


import static manifold.science.LengthUnit.Meter;

public final class AreaUnit extends AbstractProductUnit<LengthUnit, LengthUnit, Area, AreaUnit>
{
  private static final UnitCache<AreaUnit> CACHE = new UnitCache<>();

  public static final AreaUnit BASE = get( Meter, Meter );

  public static AreaUnit get( LengthUnit squareUnit )
  {
    return get( squareUnit, squareUnit, null, null, null );
  }

  public static AreaUnit get( LengthUnit widthUnit, LengthUnit lengthUnit )
  {
    return get( widthUnit, lengthUnit, null, null, null );
  }

  public static AreaUnit get( LengthUnit widthUnit, LengthUnit lengthUnit, Rational factor, String name, String symbol )
  {
    AreaUnit unit = new AreaUnit( widthUnit, lengthUnit, factor, name, symbol );
    return CACHE.get( unit );
  }

  private AreaUnit( LengthUnit widthUnit, LengthUnit lengthUnit, Rational factor, String name, String symbol )
  {
    super( widthUnit, lengthUnit == null ? widthUnit : lengthUnit, factor, name, symbol );
  }

  @Override
  public Area makeDimension( Number amount )
  {
    return new Area( Rational.get( amount ), this );
  }

  public String getFullName()
  {
    return getLengthUnit() == null
           ? getLengthUnit() + "\u00B2"
           : getWidthUnit().getFullName() + "\u00D7" + getLengthUnit().getFullName();
  }

  public String getFullSymbol()
  {
    return getLengthUnit() == null
           ? getLengthUnit() + "\u00B2"
           : getWidthUnit().getFullSymbol() + "\u00D7" + getLengthUnit().getFullSymbol();
  }

  @Override
  public String getSymbol()
  {
    if( isSquare() )
    {
      return getWidthUnit().getSymbol() + "\u00B2"; // x squared
    }
    return super.getSymbol();
  }

  public LengthUnit getWidthUnit()
  {
    return getLeftUnit();
  }

  public LengthUnit getLengthUnit()
  {
    return getRightUnit();
  }

  public boolean isSquare()
  {
    return getWidthUnit() == getLengthUnit();
  }

  public VolumeUnit times( LengthUnit lu )
  {
    return VolumeUnit.get( lu, this );
  }
}
