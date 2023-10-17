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
import java.time.LocalDateTime;

public class Oracle_TimestampLtzValueAccessor extends OtherValueAccessor
{
  // oracle-specific type
  public static final int JDBC_TYPE_TIMESTAMPLTZ = -102;
  private static final String ORACLE_SQL_TIMESTAMPLTZ = "oracle.sql.TIMESTAMPLTZ";

  @Override
  public int getJdbcType()
  {
    return JDBC_TYPE_TIMESTAMPLTZ;
  }

  @Override
  public Class<?> getJavaType( BaseElement elem )
  {
    return LocalDateTime.class;
  }

  @Override
  public Object getRowValue( ResultSet rs, BaseElement elem ) throws SQLException
  {
    Object value = super.getRowValue( rs, elem );
    if( value != null )
    {
      if( ORACLE_SQL_TIMESTAMPLTZ.equals( value.getClass().getName() ) )
      {
        Connection c = rs.getStatement().getConnection();
        c = (Connection)c.unwrap( ReflectUtil.type( "oracle.jdbc.OracleConnection" ) );
        value = ReflectUtil.method( value, "localDateTimeValue", Connection.class ).invoke( c );
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
    if( value instanceof LocalDateTime )
    {
      Connection c = ps.getConnection();
      c = (Connection)c.unwrap( ReflectUtil.type( "oracle.jdbc.OracleConnection" ) );
      value = ReflectUtil.method( ORACLE_SQL_TIMESTAMPLTZ, "of", Connection.class, LocalDateTime.class )
        .invokeStatic( c, value );
    }
    super.setParameter( ps, pos, value );
  }
}
