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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

public class Oracle_TimestampTzValueAccessor extends OtherValueAccessor
{
  // oracle-specific type
  public static final int JDBC_TYPE_TIMESTAMPTZ = -101;
  private static final String ORACLE_SQL_TIMESTAMPTZ = "oracle.sql.TIMESTAMPTZ";

  @Override
  public int getJdbcType()
  {
    return JDBC_TYPE_TIMESTAMPTZ;
  }

  @Override
  public Class<?> getJavaType( BaseElement elem )
  {
    return OffsetDateTime.class;
  }

  @Override
  public Object getRowValue( ResultSet rs, BaseElement elem ) throws SQLException
  {
    Object value = super.getRowValue( rs, elem );
    if( value != null )
    {
      if( ORACLE_SQL_TIMESTAMPTZ.equals( value.getClass().getName() ) )
      {
        Connection c = rs.getStatement().getConnection();
        c = (Connection)c.unwrap( ReflectUtil.type( "oracle.jdbc.OracleConnection" ) );
        value = ReflectUtil.method( value, "offsetDateTimeValue", Connection.class ).invoke( c );
      }
      else
      {
        throw new SQLException( "Unexpected type: " + value.getClass().getTypeName() );
      }
    }
    return value;
  }

  @Override
  public void setParameter( PreparedStatement ps, int pos, Object value ) throws SQLException
  {
    if( value instanceof OffsetDateTime )
    {
      value = ReflectUtil.method( ORACLE_SQL_TIMESTAMPTZ, "of", OffsetDateTime.class )
        .invokeStatic( value );
    }
    super.setParameter( ps, pos, value );
  }
}
