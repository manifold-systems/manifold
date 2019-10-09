package manifold.science;

import manifold.science.api.Unit;
import manifold.science.util.Rational;


import static manifold.science.util.CoercionConstants.r;

public enum ChargeUnit implements Unit<Charge, ChargeUnit>
{
  Coulomb( 1 r, "Coulomb", "C" ),
  Elementary( "1.6021766208e-19"r, "Elementary", "e" );

  public static final ChargeUnit BASE = Coulomb;

  private final Rational _coulombs;
  private final String _name;
  private final String _symbol;

  ChargeUnit( Rational coulombs, String name, String symbol )
  {
    _coulombs = coulombs;
    _name = name;
    _symbol = symbol;
  }

  public Rational getCoulombs()
  {
    return _coulombs;
  }

  @Override
  public Charge makeDimension( Number amount )
  {
    return new Charge( Rational.get( amount ), this );
  }

  public String getUnitName()
  {
    return _name;
  }

  public String getUnitSymbol()
  {
    return _symbol;
  }

  public Rational toBaseUnits( Rational myUnits )
  {
    return _coulombs * myUnits;
  }

  public Rational toNumber()
  {
    return _coulombs;
  }

  public Rational from( Charge len )
  {
    return len.toBaseNumber() / _coulombs;
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
