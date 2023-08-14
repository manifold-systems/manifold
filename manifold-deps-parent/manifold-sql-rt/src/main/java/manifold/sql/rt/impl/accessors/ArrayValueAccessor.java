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
import manifold.sql.rt.api.ValueAccessor;
import manifold.util.PrimitiveUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.*;
import java.time.*;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

public class ArrayValueAccessor implements ValueAccessor
{
  private static final Map<Class<?>, String> COMPONENT_TYPES =
    new LinkedHashMap<Class<?>, String>()
    {{
      put( Boolean.class, "BOOLEAN" );
      put( Byte.class, "TINYINT" );
      put( Short.class, "SMALLINT" );
      put( Integer.class, "INTEGER" );
      put( Long.class, "BIGINT" );
      put( Float.class, "REAL" );
      put( Double.class, "DOUBLE" );
      put( BigInteger.class, "NUMERIC" );
      put( BigDecimal.class, "NUMERIC" );
      put( Timestamp.class, "TIMESTAMP" );
      put( Instant.class, "TIMESTAMP" );
      put( LocalDateTime.class, "TIMESTAMP" );
      put( java.util.Date.class, "TIMESTAMP" );
      put( Calendar.class, "TIMESTAMP" );
      put( OffsetDateTime.class, "TIMESTAMP_WITH_TIMEZONE" );
      put( Time.class, "TIME" );
      put( LocalTime.class, "TIME" );
      put( OffsetTime.class, "TIME_WITH_TIMEZONE" );
      put( java.sql.Date.class, "DATE" );
      put( LocalDate.class, "DATE" );
      put( URL.class, "DATALINK" );
      put( String.class, "VARCHAR" );
      put( byte[].class, "VARBINARY" );
    }};

  @Override
  public int getJdbcType()
  {
    return Types.ARRAY;
  }

  @Override
  public Class<?> getJavaType( BaseElement elem )
  {
    return Object.class;
  }

  @Override
  public Object getRowValue( ResultSet rs, BaseElement elem ) throws SQLException
  {
    Array array = rs.getArray( elem.getPosition() );
    if( array == null )
    {
      return null;
    }

    try
    {
      return array.getArray();
    }
    finally
    {
      array.free();
    }
  }

  @Override
  public void setParameter( PreparedStatement ps, int pos, Object array ) throws SQLException
  {
    if( array == null )
    {
      ps.setNull( pos, getJdbcType() );
    }
    else
    {
      Class<?> arrayType = array.getClass();
      if( !arrayType.isArray() )
      {
        throw new SQLException( "Expecting array type, but found: " + arrayType.getTypeName() );
      }

      if( arrayType.isPrimitive() )
      {
        // For some reason createArrayOf() only takes Object[], not primitive arrays,
        // so we have to make a boxed version of a primitive array
        array = makeBoxedArray( array, arrayType );
        arrayType = array.getClass();
      }

      Class<?> componentType = arrayType.getComponentType();
      String jdbcType = COMPONENT_TYPES.get( componentType );
      Array jdbcArray = ps.getConnection().createArrayOf( jdbcType, (Object[])array );
      try
      {
        ps.setArray( pos, jdbcArray );
      }
      finally
      {
        jdbcArray.free();
      }
    }
  }

  private Object[] makeBoxedArray( Object primitiveArray, Class<?> valueClass )
  {
    Class boxedType = PrimitiveUtil.getBoxedType( valueClass );
    int len = java.lang.reflect.Array.getLength( primitiveArray );
    Object[] boxedArray = (Object[])java.lang.reflect.Array.newInstance( boxedType, len );
    for( int i = 0; i < len; i++ )
    {
      boxedArray[i] = java.lang.reflect.Array.get( primitiveArray, i );
    }
    return boxedArray;
  }
}
