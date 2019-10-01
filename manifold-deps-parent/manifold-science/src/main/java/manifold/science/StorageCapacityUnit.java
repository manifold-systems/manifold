package manifold.science;

import manifold.science.api.IUnit;
import manifold.science.util.Rational;


import static manifold.science.MetricScaleUnit.r;
import static manifold.science.util.CommonConstants.KIBI;
import static manifold.science.util.CommonConstants.KILO;

public enum StorageCapacityUnit implements IUnit<StorageCapacity, StorageCapacityUnit>
{
  Bit( Rational.get( 1, 8 ), "Bit", "bit" ),
  Nibble( Rational.HALF, "Nibble", "nibble" ),
  Byte( 1r, "Byte", "B" ),
  KB( KILO, "Kilobyte", "KB" ),
  KiB( KIBI, "Kibibyte", "KiB" ),
  MB( KILO.pow( 2 ), "Megabyte", "MB" ),
  MiB( KIBI.pow( 2 ), "Mebibyte", "MiB" ),
  GB( KILO.pow( 3 ), "Gigabyte", "GB" ),
  GiB( KIBI.pow( 3 ), "Gibibyte", "GiB" ),
  TB( KILO.pow( 4 ), "Terabyte", "TB" ),
  TiB( KIBI.pow( 4 ), "Tebibyte", "TiB" ),
  PB( KILO.pow( 5 ), "Petabyte", "TB" ),
  PiB( KIBI.pow( 5 ), "Pebibyte", "TiB" ),
  EB( KILO.pow( 6 ), "Exabyte", "EB" ),
  EiB( KIBI.pow( 6 ), "Exbibyte", "EiB" ),
  ZB( KILO.pow( 7 ), "Zettabyte", "ZB" ),
  ZiB( KIBI.pow( 7 ), "Zebibyte", "ZiB" ),
  YB( KILO.pow( 8 ), "Yottabyte", "YB" ),
  YiB( KIBI.pow( 8 ), "Yobibyte", "YiB" );

  private final Rational _bytes;
  private final String _name;
  private final String _symbol;

  public static final StorageCapacityUnit BASE = Byte;

  StorageCapacityUnit( Rational bytes, String name, String symbol ) {
    _bytes = bytes;
    _name = name;
    _symbol = symbol;
  }

  @Override
  public StorageCapacity makeDimension( Number amount )
  {
    return new StorageCapacity( Rational.get( amount ), this );
  }

  public Rational getBytes()
  {
    return _bytes;
  }

  public String getUnitName() {
    return _name;
  }

   public String getUnitSymbol() {
    return _symbol;
  }

  public Rational toBaseUnits( Rational myUnits ) {
    return _bytes * myUnits;
  }

  public Rational toNumber() {
    return _bytes;
  }

  public Rational from( StorageCapacity bytes ) {
    return bytes.toBaseNumber() / _bytes;
  }
}
