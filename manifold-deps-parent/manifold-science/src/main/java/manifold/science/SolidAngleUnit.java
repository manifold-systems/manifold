package manifold.science;

import manifold.science.api.AbstractPrimaryUnit;
import manifold.science.api.UnitCache;
import manifold.science.util.Rational;


import static manifold.science.util.CoercionConstants.r;

public final class SolidAngleUnit extends AbstractPrimaryUnit<SolidAngle, SolidAngleUnit>
{
  private static final UnitCache<SolidAngleUnit> CACHE = new UnitCache<>();
  public static SolidAngleUnit get( Rational seconds, String name, String symbol )
  {
    return CACHE.get( new SolidAngleUnit( seconds, name, symbol ) );
  }

  public static final SolidAngleUnit Steradian = get( 1r, "Steradian", "sr" );

  public static final SolidAngleUnit BASE = Steradian;

  private SolidAngleUnit( Rational sr, String name, String symbol )
  {
    super( sr, name, symbol );
  }

  @Override
  public SolidAngle makeDimension( Number amount )
  {
    return new SolidAngle( Rational.get( amount ), this );
  }
}
