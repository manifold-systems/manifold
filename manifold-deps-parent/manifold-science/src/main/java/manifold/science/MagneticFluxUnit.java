package manifold.science;

import manifold.science.api.AbstractQuotientUnit;
import manifold.science.api.UnitCache;
import manifold.science.util.Rational;


import static manifold.science.util.CoercionConstants.r;

public final class MagneticFluxUnit extends AbstractQuotientUnit<EnergyUnit, CurrentUnit, MagneticFlux, MagneticFluxUnit>
{
  private static final UnitCache<MagneticFluxUnit> CACHE = new UnitCache<>();

  public static final MagneticFluxUnit Wb = get( EnergyUnit.BASE, CurrentUnit.BASE, 1 r, "Weber", "Wb" );

  public static final MagneticFluxUnit BASE = Wb;

  public static MagneticFluxUnit get( EnergyUnit energyUnit, CurrentUnit currentUnit )
  {
    return get( energyUnit, currentUnit, null, null, null );
  }

  public static MagneticFluxUnit get( EnergyUnit energyUnit, CurrentUnit currentUnit, Rational factor, String name, String symbol )
  {
    MagneticFluxUnit unit = new MagneticFluxUnit( energyUnit, currentUnit, factor, name, symbol );
    return CACHE.get( unit );
  }

  private MagneticFluxUnit( EnergyUnit energyUnit, CurrentUnit currentUnit, Rational factor, String name, String symbol )
  {
    super( energyUnit, currentUnit, factor, name, symbol );
  }

  @Override
  public MagneticFlux makeDimension( Number amount )
  {
    return new MagneticFlux( Rational.get( amount ), this );
  }

  public EnergyUnit getEnergyUnit()
  {
    return getLeftUnit();
  }

  public CurrentUnit getCurrentUnit()
  {
    return getRightUnit();
  }

  public MagneticFluxDensityUnit div( AreaUnit area )
  {
    return MagneticFluxDensityUnit.get( this, area );
  }

  public AreaUnit div( MagneticFluxDensityUnit mf )
  {
    return mf.getAreaUnit();
  }
}
