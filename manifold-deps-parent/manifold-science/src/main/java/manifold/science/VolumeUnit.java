package manifold.science;

import manifold.science.api.AbstractProductUnit;
import manifold.science.api.UnitCache;
import manifold.science.util.Rational;


import static manifold.science.LengthUnit.Centi;
import static manifold.science.LengthUnit.Meter;
import static manifold.science.MetricScaleUnit.r;
import static manifold.science.util.CommonConstants.KILO;

public final class VolumeUnit extends AbstractProductUnit<LengthUnit, AreaUnit, Volume, VolumeUnit>
{
  private static final UnitCache<VolumeUnit> CACHE = new UnitCache<>();

  public static final VolumeUnit BASE = get( Meter, AreaUnit.get( Meter ) );

  public static final VolumeUnit LITER = get( Centi, AreaUnit.get( Centi ), KILO, "Litre", "L" );
  public static final VolumeUnit MILLI_LITER = get( Centi, AreaUnit.get( Centi ), 1r, "Millilitre", "mL" );
  public static final VolumeUnit FLUID_OZ = get( Centi, AreaUnit.get( Centi ), "29.5735295625"r, "Fluid Ounce", "fl oz." );
  public static final VolumeUnit GALLON = get( Centi, AreaUnit.get( Centi ), "3785.411784"r, "Gallon", "gal." );
  public static final VolumeUnit QUART = get( Centi, AreaUnit.get( Centi ), "946.352946"r, "Quart", "qt." );
  public static final VolumeUnit PINT = get( Centi, AreaUnit.get( Centi ), "473.176473"r, "Pint", "pt." );
  public static final VolumeUnit CUP = get( Centi, AreaUnit.get( Centi ), "236.5882365"r, "Cup", "c." );
  public static final VolumeUnit TABLE_SPOON = get( Centi, AreaUnit.get( Centi ), "14.78676478125"r, "Tablespoon", "tbsp" );
  public static final VolumeUnit TEA_SPOON = get( Centi, AreaUnit.get( Centi ), "4.92892159375"r, "Teaspoon", "tsp" );

  public static VolumeUnit get( LengthUnit lengthUnit, AreaUnit areaUnit ) {
    return get( lengthUnit, areaUnit, null, null, null );
  }
  public static VolumeUnit get( LengthUnit lengthUnit, AreaUnit areaUnit, Rational factor, String name, String symbol ) {
    VolumeUnit unit = new VolumeUnit( lengthUnit, areaUnit, factor, name, symbol );
    return CACHE.get( unit );
  }
  
  private VolumeUnit( LengthUnit lengthUnit, AreaUnit areaUnit, Rational factor, String name, String symbol ) {
    super( lengthUnit, areaUnit == null ? AreaUnit.get( lengthUnit ) : areaUnit, factor, name, symbol );
  }

  @Override
  public Volume makeDimension( Number amount )
  {
    return new Volume( Rational.get( amount ), this );
  }

  public AreaUnit getAreaUnit() {
    return getRightUnit();
  }
  public LengthUnit getLengthUnit() {
    return getLeftUnit();
  }

  public String getFullName() {
    return getAreaUnit().isSquare() && getAreaUnit().getWidthUnit() == getLengthUnit()
             ? getLengthUnit().getFullName() + "\u00B3"
             : getAreaUnit().getFullName() + "\u00D7" + getLengthUnit().getFullName();
  }
  
  public String getFullSymbol() {
    return getAreaUnit().isSquare() && getAreaUnit().getWidthUnit() == getLengthUnit()
             ? getLengthUnit().getFullSymbol() + "\u00B3"
             : getAreaUnit().getFullSymbol() + "\u00D7" + getLengthUnit().getFullSymbol();
  }

  public boolean getIsCubic() {
    return getAreaUnit().isSquare() && getAreaUnit().getWidthUnit() == getLengthUnit();
  }

  public LengthUnit divide( AreaUnit len ) {
    return getLengthUnit();
  }

  public MassUnit multiply( DensityUnit density ) {
    return density.getMassUnit();
  }
}
