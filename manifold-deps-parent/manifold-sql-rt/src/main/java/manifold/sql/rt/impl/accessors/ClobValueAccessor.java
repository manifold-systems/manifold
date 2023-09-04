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

import java.io.StringReader;
import java.sql.*;

public class ClobValueAccessor implements ValueAccessor
{
  @Override
  public int getJdbcType()
  {
    return Types.CLOB;
  }

  @Override
  public Class<?> getJavaType( BaseElement elem )
  {
    return String.class;
  }

  @Override
  public String getRowValue( ResultSet rs, BaseElement elem ) throws SQLException
  {
    Object res = rs.getObject( elem.getPosition() );
    if( res == null )
    {
      return null;
    }

    if( res instanceof String )
    {
      return (String)res;
    }

    if( res instanceof Clob )
    {
      Clob clob = (Clob)res;
      try
      {
        return clob.getSubString( 1, (int)clob.length() );
      }
      finally
      {
        clob.free();
      }
    }

    if( res instanceof char[] )
    {
      return String.valueOf( (char[])res );
    }

    throw new SQLException( "Expecting a clob or String, but found: " + res.getClass() );
  }

  @Override
  public void setParameter( PreparedStatement ps, int pos, Object value ) throws SQLException
  {
    if( value == null )
    {
      ps.setNull( pos, getJdbcType() );
    }
    else if( value instanceof String )
    {
      String string = (String)value;
      StringReader reader = new StringReader( string );
      ps.setCharacterStream( pos, reader, string.length() );
    }
    else
    {
      ps.setObject( pos, value, getJdbcType() );
    }
  }
}
