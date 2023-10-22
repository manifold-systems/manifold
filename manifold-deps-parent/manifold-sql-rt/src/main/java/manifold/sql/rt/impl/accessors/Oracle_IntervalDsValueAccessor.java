/*
 * Copyright (c) 2023 - Manifold Systems LLC
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

package manifold.sql.rt.impl.accessors;

import manifold.sql.rt.api.BaseElement;
import manifold.util.ReflectUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;

import static java.lang.Byte.toUnsignedInt;
import static java.lang.Math.toIntExact;

public class Oracle_IntervalDsValueAccessor extends OtherValueAccessor
{
  // oracle-specific type
  public static final int JDBC_TYPE_INTERVALDS = -104;
  private static final String JAVA_TYPE_INTERVALDS = "oracle.sql.INTERVALDS";
  private static final int SIZE_INTERVALDS = 11;
  private static final int HIGH_BIT_FLAG = 0x80000000;

  @Override
  public int getJdbcType()
  {
    return JDBC_TYPE_INTERVALDS;
  }

  @Override
  public Class<?> getJavaType( BaseElement elem )
  {
    return Duration.class;
  }

  @Override
  public Object getRowValue( ResultSet rs, BaseElement elem ) throws SQLException
  {
    Object value = super.getRowValue( rs, elem );
    if( value != null && value.getClass().getTypeName().equals( JAVA_TYPE_INTERVALDS ) )
    {
      value = toDuration( value );
    }
    return value;
  }

  @Override
  public void setParameter( PreparedStatement ps, int pos, Object value ) throws SQLException
  {
    if( value instanceof Duration )
    {
      value = toIntervalDs( (Duration)value );
    }
    super.setParameter( ps, pos, value );
  }

  private Duration toDuration( Object/*INTERVALDS*/ value )
  {
    byte[] bytes = (byte[])ReflectUtil.method( value, "toBytes" ).invoke();
    int day = toUnsignedInt( bytes[0] ) << 24
      | toUnsignedInt( bytes[1] ) << 16
      | toUnsignedInt( bytes[2] ) << 8
      | toUnsignedInt( bytes[3] );
    day ^= HIGH_BIT_FLAG;
    int hour = toUnsignedInt( bytes[4] ) - 60;
    int minute = toUnsignedInt( bytes[5] ) - 60;
    int second = toUnsignedInt( bytes[6] ) - 60;
    int nano = toUnsignedInt( bytes[7] ) << 24
      | toUnsignedInt( bytes[8] ) << 16
      | toUnsignedInt( bytes[9] ) << 8
      | toUnsignedInt( bytes[10] );
    nano ^= HIGH_BIT_FLAG;
    return Duration.ofDays( day )
      .plusHours( hour )
      .plusMinutes( minute )
      .plusSeconds( second )
      .plusNanos( nano );
  }

  private Object/*INTERVALDS*/ toIntervalDs( Duration duration )
  {
    long seconds = duration.getSeconds();
    if( duration.isNegative() )
    {
      seconds += 1L;
    }
    int day = toIntExact( seconds / 24L / 60L / 60L );
    int hour = (int)((seconds / 60L / 60L) - (day * 24L));
    int minute = (int)((seconds / 60L) - (day * 24L * 60L) - (hour * 60L));
    int second = (int)(seconds % 60);
    int nano;
    if( duration.isNegative() )
    {
      // Java represents -10.1 seconds as
      //  -11 seconds
      //  +900000000 nanoseconds
      // Oracle wants -10.1 seconds as
      //  -10 seconds
      //  -100000000 nanoseconds
      nano = duration.getNano() - 1_000_000_000;
    }
    else
    {
      nano = duration.getNano();
    }
    nano ^= HIGH_BIT_FLAG;
    day ^= HIGH_BIT_FLAG;

    hour += 60;
    minute += 60;
    second += 60;

    byte[] bytes = new byte[SIZE_INTERVALDS];
    bytes[0] = (byte)(day >> 24);
    bytes[1] = (byte)(day >> 16 & 0xFF);
    bytes[2] = (byte)(day >> 8 & 0xFF);
    bytes[3] = (byte)(day & 0xFF);

    bytes[4] = (byte)(hour & 0xFF);
    bytes[5] = (byte)(minute & 0xFF);
    bytes[6] = (byte)(second & 0xFF);

    bytes[7] = (byte)(nano >> 24);
    bytes[8] = (byte)(nano >> 16 & 0xFF);
    bytes[9] = (byte)(nano >> 8 & 0xFF);
    bytes[10] = (byte)(nano & 0xFF);

    //noinspection RedundantCast
    return ReflectUtil.constructor( JAVA_TYPE_INTERVALDS, byte[].class ).newInstance( (Object)bytes );
  }
}
