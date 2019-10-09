package manifold.science;

import manifold.science.api.AbstractQuotientUnit;
import manifold.science.api.UnitCache;
import manifold.science.util.Rational;


import static manifold.science.MassUnit.Kilogram;

public final class DensityUnit extends AbstractQuotientUnit<MassUnit, VolumeUnit, Density, DensityUnit>
{
  private static final UnitCache<DensityUnit> CACHE = new UnitCache<>();

  public static final DensityUnit BASE = get( Kilogram, VolumeUnit.BASE );

  public static DensityUnit get( MassUnit massUnit, VolumeUnit volumeUnit )
  {
    return get( massUnit, volumeUnit, null, null, null );
  }

  public static DensityUnit get( MassUnit massUnit, VolumeUnit volumeUnit, Rational factor, String name, String symbol )
  {
    DensityUnit unit = new DensityUnit( massUnit, volumeUnit, factor, name, symbol );
    return CACHE.get( unit );
  }

  private DensityUnit( MassUnit massUnit, VolumeUnit volumeUnit, Rational factor, String name, String symbol )
  {
    super( massUnit, volumeUnit, factor, name, symbol );
  }

  @Override
  public Density makeDimension( Number amount )
  {
    return new Density( Rational.get( amount ), this );
  }

  public MassUnit getMassUnit()
  {
    return getLeftUnit();
  }

  public VolumeUnit getVolumeUnit()
  {
    return getRightUnit();
  }
}
