package manifold.science;

import manifold.science.api.Unit;
import manifold.science.util.Rational;


import static manifold.science.MetricScaleUnit.r;

public enum SolidAngleUnit implements Unit<SolidAngle, SolidAngleUnit>
{
  Steradian( 1r, "Steradian", "sr" );

  private final Rational _sr;
  private final String _name;
  private final String _symbol;

  public static final SolidAngleUnit BASE = Steradian;

  SolidAngleUnit( Rational sr, String name, String symbol ) {
    _sr = sr;
    _name = name;
    _symbol = symbol;
  }

  @Override
  public SolidAngle makeDimension( Number amount )
  {
    return new SolidAngle( Rational.get( amount ), this );
  }

  public String getUnitName() {
    return _name;
  }

   public String getUnitSymbol() {
    return _symbol;
  }

  public Rational toBaseUnits( Rational myUnits ) {
    return _sr * myUnits;
  }

  public Rational toNumber() {
    return _sr;
  }

  public Rational from( SolidAngle len ) {
    return len.toBaseNumber() / _sr;
  }
}
