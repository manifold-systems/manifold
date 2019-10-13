/*
 * Copyright (c) 2019 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.science.measures;

import manifold.science.api.AbstractPrimaryUnit;
import manifold.science.api.UnitCache;
import manifold.science.util.Rational;


import static manifold.science.util.CoercionConstants.r;
import static manifold.science.util.MetricFactorConstants.KIBI;
import static manifold.science.util.MetricFactorConstants.KILO;

public final class StorageCapacityUnit extends AbstractPrimaryUnit<StorageCapacity, StorageCapacityUnit>
{
  private static final UnitCache<StorageCapacityUnit> CACHE = new UnitCache<>();
  public static StorageCapacityUnit get( Rational bytes, String name, String symbol )
  {
    return CACHE.get( new StorageCapacityUnit( bytes, name, symbol ) );
  }

  public static final StorageCapacityUnit Bit = get( 1r/8, "Bit", "bit" );
  public static final StorageCapacityUnit Nibble = get( Rational.HALF, "Nibble", "nibble" );
  public static final StorageCapacityUnit Byte = get( Rational.ONE, "Byte", "B" );
  public static final StorageCapacityUnit KB = get( KILO, "Kilobyte", "KB" );
  public static final StorageCapacityUnit KiB = get( KIBI, "Kibibyte", "KiB" );
  public static final StorageCapacityUnit MB = get( KILO.pow( 2 ), "Megabyte", "MB" );
  public static final StorageCapacityUnit MiB = get( KIBI.pow( 2 ), "Mebibyte", "MiB" );
  public static final StorageCapacityUnit GB = get( KILO.pow( 3 ), "Gigabyte", "GB" );
  public static final StorageCapacityUnit GiB = get( KIBI.pow( 3 ), "Gibibyte", "GiB" );
  public static final StorageCapacityUnit TB = get( KILO.pow( 4 ), "Terabyte", "TB" );
  public static final StorageCapacityUnit TiB = get( KIBI.pow( 4 ), "Tebibyte", "TiB" );
  public static final StorageCapacityUnit PB = get( KILO.pow( 5 ), "Petabyte", "TB" );
  public static final StorageCapacityUnit PiB = get( KIBI.pow( 5 ), "Pebibyte", "TiB" );
  public static final StorageCapacityUnit EB = get( KILO.pow( 6 ), "Exabyte", "EB" );
  public static final StorageCapacityUnit EiB = get( KIBI.pow( 6 ), "Exbibyte", "EiB" );
  public static final StorageCapacityUnit ZB = get( KILO.pow( 7 ), "Zettabyte", "ZB" );
  public static final StorageCapacityUnit ZiB = get( KIBI.pow( 7 ), "Zebibyte", "ZiB" );
  public static final StorageCapacityUnit YB = get( KILO.pow( 8 ), "Yottabyte", "YB" );
  public static final StorageCapacityUnit YiB = get( KIBI.pow( 8 ), "Yobibyte", "YiB" );

  public static final StorageCapacityUnit BASE = Byte;

  private StorageCapacityUnit( Rational bytes, String name, String symbol )
  {
    super( bytes, name, symbol );
  }

  @Override
  public StorageCapacity makeDimension( Number amount )
  {
    return new StorageCapacity( Rational.get( amount ), this );
  }
}
