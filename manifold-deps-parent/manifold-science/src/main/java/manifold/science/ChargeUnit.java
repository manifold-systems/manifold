package manifold.science;

import manifold.science.api.IUnit;
import manifold.science.util.Rational;


import static manifold.science.MetricScaleUnit.r;

public enum ChargeUnit implements IUnit<Charge, ChargeUnit>
{
  Coulomb( 1r, "Coulomb", "C" ),
  Elementary( "1.6021766208e-19"r, "Elementary", "e" );

  public static final ChargeUnit BASE = Coulomb;

  private final Rational _coulombs;
  private final String _name;
  private final String _symbol;

  ChargeUnit( Rational coulombs, String name, String symbol ) {
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

  public String getUnitName() {
    return _name;
  }

   public String getUnitSymbol() {
    return _symbol;
  }

  public Rational toBaseUnits( Rational myUnits ) {
    return _coulombs * myUnits;
  }

  public Rational toNumber() {
    return _coulombs;
  }

  public Rational from( Charge len ) {
    return len.toBaseNumber() / _coulombs;
  }

  public CurrentUnit divide( TimeUnit time ) {
    return CurrentUnit.get( this, time );
  }

  public TimeUnit divide( CurrentUnit i ) {
    return i.getTimeUnit();
  }

  public CapacitanceUnit divide( PotentialUnit p ) {
    return CapacitanceUnit.get( this, p );
  }
  public PotentialUnit divide( CapacitanceUnit cu ) {
    return cu.getPotentialUnit();
  }
}
