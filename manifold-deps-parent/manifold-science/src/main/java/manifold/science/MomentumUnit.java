package manifold.science;

import manifold.science.api.AbstractProductUnit;
import manifold.science.api.UnitCache;
import manifold.science.util.Rational;


import static manifold.science.MassUnit.Kilogram;

public final class MomentumUnit extends AbstractProductUnit<MassUnit, VelocityUnit, Momentum, MomentumUnit>
{
  private static final UnitCache<MomentumUnit> CACHE = new UnitCache<>();

  public static final MomentumUnit BASE = get( Kilogram, VelocityUnit.BASE );

  public static MomentumUnit get( MassUnit massUnit, VelocityUnit velocityUnit ) {
    return get( massUnit, velocityUnit, null, null, null );
  }
  public static MomentumUnit get( MassUnit massUnit, VelocityUnit velocityUnit, Rational factor, String name, String symbol ) {
    MomentumUnit unit = new MomentumUnit( massUnit, velocityUnit, factor, name, symbol );
    return CACHE.get( unit );
  }
  
  private MomentumUnit( MassUnit massUnit, VelocityUnit velocityUnit, Rational factor, String name, String symbol ) {
    super( massUnit, velocityUnit, factor, name, symbol );
  }

  @Override
  public Momentum makeDimension( Number amount )
  {
    return new Momentum( Rational.get( amount ), this );
  }

  public MassUnit getMassUnit() {
    return getLeftUnit();
  }
  public VelocityUnit getVelocityUnit() {
    return getRightUnit();
  }
  
  public EnergyUnit multiply( VelocityUnit v ) {
    return EnergyUnit.get( getMassUnit() * (getVelocityUnit() / v.getTimeUnit()), v.getLengthUnit() );
  }
    
  public MassUnit divide( VelocityUnit w ) {
    return getMassUnit();
  }  
}
