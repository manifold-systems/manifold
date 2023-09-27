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

import java.sql.*;

public class OtherValueAccessor implements ValueAccessor
{
  @Override
  public int getJdbcType()
  {
    return Types.OTHER;
  }

  @Override
  public Class<?> getJavaType( BaseElement elem )
  {
    return getClassForColumnClassName( elem.getColumnClassName(), Object.class );
  }

  @Override
  public Object getRowValue( ResultSet rs, BaseElement elem ) throws SQLException
  {
    Object postgresStrangeness = postgresStrangeness( rs, elem );
    if( postgresStrangeness != NONE )
    {
      return postgresStrangeness;
    }
    return rs.getObject( elem.getPosition() );
  }

  @Override
  public void setParameter( PreparedStatement ps, int pos, Object value ) throws SQLException
  {
    if( value == null )
    {
      ps.setNull( pos, getJdbcType() );
    }
    else
    {
      ps.setObject( pos, value );
    }
  }

  @Override
  public String getParameterExpression( DatabaseMetaData metaData, Object value, ColumnInfo ci )
  {
    // This is a special case for Postgres. It requires casts for some data types :\
    // See also BitValueAccessor for more of the same.
    try
    {
      if( metaData.getDatabaseProductName().equalsIgnoreCase( "postgresql" ) )
      {
        String lcSqlType = ci.getSqlType().toLowerCase();
        switch( lcSqlType )
        {
          case "cidr":
          case "inet":
          case "macaddr":
          case "macaddr8":
          case "money":
          case "varbit":
          case "bit varying":
            return castParam( value, ci );
        }
      }
    }
    catch( SQLException e )
    {
      throw new RuntimeException( e );
    }
    return ValueAccessor.super.getParameterExpression( metaData, value, ci );
  }

  private String castParam( Object value, ColumnInfo ci )
  {
    return "CAST(? AS " + ci.getSqlType() + ")";
  }

  // note, this is for postgres because it returns PGobject instances for `varbit` etc.,
  // but for giggles postgres does not return PGobject.class for varbit's java class.
  private static final Object NONE = new Object() {};
  private static Object postgresStrangeness( ResultSet rs, BaseElement elem ) throws SQLException
  {
    String lcSqlType = elem.getSqlType().toLowerCase();
    switch( lcSqlType )
    {
      case "cidr":
      case "inet":
      case "macaddr":
      case "macaddr8":
      case "varbit":
      case "bit varying":
        return rs.getString( elem.getPosition() );
      case "money":
        return rs.getDouble( elem.getPosition() );
    }
    return NONE;
  }

}
