package manifold.science;

import manifold.science.api.AbstractPrimaryUnit;
import manifold.science.api.UnitCache;
import manifold.science.util.Rational;


import static manifold.science.util.CoercionConstants.r;

public final class ChargeUnit extends AbstractPrimaryUnit<Charge, ChargeUnit>
{
  private static final UnitCache<ChargeUnit> CACHE = new UnitCache<>();
  public static ChargeUnit get( Rational coulombs, String name, String symbol )
  {
    return CACHE.get( new ChargeUnit( coulombs, name, symbol ) );
  }

  public static final ChargeUnit Coulomb = get( 1r, "Coulomb", "C" );
  public static final ChargeUnit Elementary = get( 1.6021766208e-19r, "Elementary", "e" );

  public static final ChargeUnit BASE = Coulomb;

  public ChargeUnit( Rational coulombs, String name, String symbol )
  {
    super( coulombs, name, symbol );
  }

  public Rational getCoulombs()
  {
    return toNumber();
  }

  @Override
  public Charge makeDimension( Number amount )
  {
    return new Charge( Rational.get( amount ), this );
  }

  public CurrentUnit div( TimeUnit time )
  {
    return CurrentUnit.get( this, time );
  }

  public TimeUnit div( CurrentUnit i )
  {
    return i.getTimeUnit();
  }

  public CapacitanceUnit div( PotentialUnit p )
  {
    return CapacitanceUnit.get( this, p );
  }

  public PotentialUnit div( CapacitanceUnit cu )
  {
    return cu.getPotentialUnit();
  }
}
