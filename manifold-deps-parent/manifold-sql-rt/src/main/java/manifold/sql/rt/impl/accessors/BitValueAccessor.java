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
import manifold.sql.rt.api.ColumnInfo;
import manifold.sql.rt.api.ValueAccessor;
import manifold.sql.rt.util.DriverInfo;

import java.sql.*;

import static manifold.sql.rt.util.DriverInfo.MySQL;
import static manifold.sql.rt.util.DriverInfo.Postgres;

public class BitValueAccessor implements ValueAccessor
{
  @Override
  public int getJdbcType()
  {
    return Types.BIT;
  }

  @Override
  public Class<?> getJavaType( BaseElement elem )
  {
    int size = elem.getSize();
    if( size > 1 ||
      // some DBs such as duckdb treat BIT as variable length BITSTRING when no size is present
      String.class.getTypeName().equals( elem.getColumnClassName() ) )
    {
      // a bitstring such as "10110011"
      return String.class;
    }
    return elem.canBeNull() ? Boolean.class : boolean.class;
  }

  @Override
  public Object getRowValue( ResultSet rs, BaseElement elem ) throws SQLException
  {
    if( elem.getSize() > 1 )
    {
      DriverInfo d = DriverInfo.lookup( rs.getStatement().getConnection().getMetaData() );
      if( d == MySQL )
      {
        return rs.getBigDecimal( elem.getPosition() ).toBigInteger().toString( 2 );
      }
      return rs.getString( elem.getPosition() );
    }
    boolean value = rs.getBoolean( elem.getPosition() );
    return !value && rs.wasNull() ? null : value;
  }

  @Override
  public void setParameter( PreparedStatement ps, int pos, Object value ) throws SQLException
  {
    if( value == null )
    {
      ps.setNull( pos, getJdbcType() );
    }
    else if( value instanceof byte[] )
    {
      ps.setBytes( pos, (byte[])value );
    }
    else if( value instanceof Boolean )
    {
      ps.setBoolean( pos, (boolean)value );
    }
    else
    {
      ps.setObject( pos, value, getJdbcType() );
    }
  }

  @Override
  public String getParameterExpression( DatabaseMetaData metaData, Object value, ColumnInfo ci )
  {
    // This is a special case for Postgres. It requires casts for some data types such as `bit` :\
    // See OtherValueAccessor for more of the same.
    // Note, SQL cast expr does not work here, hence the literal value expressions.
    try
    {
      DriverInfo driver = DriverInfo.lookup( metaData );
      if( driver == Postgres || driver == MySQL )
      {
        if( !ci.getSqlType().toLowerCase().contains( "bool" ) )
        {
          // note, checking for "bool" because postgres assigns "bit" jdbc type to "bool" sql types,
          // and then throws exceptions about this :\

          // "bit" types must be manually parameterized with postgres :(
          return coerce( driver, value, ci );
        }
      }
    }
    catch( SQLException e )
    {
      throw new RuntimeException( e );
    }
    return "?";
  }

  private String coerce( DriverInfo driver, Object value, ColumnInfo ci ) throws SQLException
  {
    if( value == null )
    {
      return "NULL";
    }
    if( value instanceof Boolean )
    {
      return "B'" + (((boolean)value) ? '1' : '0') + "'";
    }
    if( value instanceof CharSequence )
    {
      return "B'" + value + "'" + cast( driver, ci );
    }
    throw new SQLException( "Unexpected type for BIT: " + value.getClass() );
  }

  private String cast( DriverInfo driver, ColumnInfo ci )
  {
    if( driver != Postgres )
    {
      // only postgres requires a cast
      return "";
    }

    Integer size = ci.getSize();
    if( size != null && size.intValue() > 0 )
    {
      return "::" + ci.getSqlType() + "(" + size.intValue() + ")";
    }
    return "";
  }
}
