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

import java.sql.*;
import java.time.LocalDate;

public class DateValueAccessor implements ValueAccessor
{
  @Override
  public int getJdbcType()
  {
    return Types.DATE;
  }

  @Override
  public Class<?> getJavaType( BaseElement elem )
  {
    return LocalDate.class;
  }

  @Override
  public LocalDate getRowValue( ResultSet rs, BaseElement elem ) throws SQLException
  {
    Date date = rs.getDate( elem.getPosition() );
    return date == null ? null : date.toLocalDate();
  }

  @Override
  public void setParameter( PreparedStatement ps, int pos, Object value ) throws SQLException
  {
    if( value == null )
    {
      ps.setNull( pos, getJdbcType() );
    }
    else if( value instanceof LocalDate )
    {
      ps.setDate( pos, Date.valueOf( (LocalDate)value ) );
    }
    else
    {
      ps.setObject( pos, value, getJdbcType() );
    }
  }
}
