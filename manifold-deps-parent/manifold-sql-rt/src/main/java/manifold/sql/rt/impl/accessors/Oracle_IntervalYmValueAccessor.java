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
import java.time.Period;

import static java.lang.Byte.toUnsignedInt;

public class Oracle_IntervalYmValueAccessor extends OtherValueAccessor
{
  // oracle-specific type
  public static final int JDBC_TYPE_INTERVALYM = -103;
  private static final String JAVA_TYPE_INTERVALYM = "oracle.sql.INTERVALYM";
  private static final int SIZE_INTERVALYM = 5;
  private static final int HIGH_BIT = 0x80000000;

  @Override
  public int getJdbcType()
  {
    return JDBC_TYPE_INTERVALYM;
  }

  @Override
  public Class<?> getJavaType( BaseElement elem )
  {
    return Period.class;
  }

  @Override
  public Object getRowValue( ResultSet rs, BaseElement elem ) throws SQLException
  {
    Object value = super.getRowValue( rs, elem );
    if( value != null && value.getClass().getTypeName().equals( JAVA_TYPE_INTERVALYM ) )
    {
      value = toPeriod( value );
    }
    return value;
  }

  @Override
  public void setParameter( PreparedStatement ps, int pos, Object value ) throws SQLException
  {
    if( value instanceof Period )
    {
      value = toIntervalYm( (Period)value );
    }
    super.setParameter( ps, pos, value );
  }

  private Period toPeriod( Object/*INTERVALYM*/ value )
  {
    byte[] bytes = (byte[])ReflectUtil.method( value, "toBytes" ).invoke();
    int year = toUnsignedInt( bytes[0] ) << 24
      | toUnsignedInt( bytes[1] ) << 16
      | toUnsignedInt( bytes[2] ) << 8
      | toUnsignedInt( bytes[3] );
    year ^= HIGH_BIT;
    int month = toUnsignedInt( bytes[4] ) - 60;
    return Period.of( year, month, 0 );
  }

  private Object/*INTERVALYM*/ toIntervalYm( Period period )
  {
    int year = period.getYears() ^ HIGH_BIT;
    byte[] bytes = new byte[SIZE_INTERVALYM];
    bytes[0] = (byte)(year >> 24);
    bytes[1] = (byte)(year >> 16 & 0xFF);
    bytes[2] = (byte)(year >> 8 & 0xFF);
    bytes[3] = (byte)(year & 0xFF);

    int month = period.getMonths() + 60;
    bytes[4] = (byte)(month & 0xFF);

    //noinspection RedundantCast
    return ReflectUtil.constructor( JAVA_TYPE_INTERVALYM, byte[].class ).newInstance( (Object)bytes );
  }
}
