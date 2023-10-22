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
import java.time.Year;
import java.time.YearMonth;

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
    if( elem.getSqlType().equalsIgnoreCase( "year" ) )
    {
      return Year.class;
    }
    if( elem.getSqlType().equalsIgnoreCase( "yearmonth" ) )
    {
      return YearMonth.class;
    }
    return LocalDate.class;
  }

  @Override
  public Object getRowValue( ResultSet rs, BaseElement elem ) throws SQLException
  {
    Date date = rs.getDate( elem.getPosition() );
    if( date == null )
    {
      return null;
    }

    String sqlType = elem.getSqlType();
    if( sqlType.equalsIgnoreCase( "year" ) )
    {
      return Year.of( date.toLocalDate().getYear() );
    }
    if( sqlType.equalsIgnoreCase( "yearmonth" ) )
    {
      LocalDate localDate = date.toLocalDate();
      return YearMonth.of( localDate.getYear(), localDate.getMonth() );
    }
    return date.toLocalDate();
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
    else if( value instanceof Year )
    {
// this should work, but does not for some drivers (MySql)
//      Date year = Date.valueOf( ((Year)value).atDay( 1 ) );
//      ps.setDate( pos, year );
      ps.setShort( pos, (short)((Year)value).getValue() );
    }
    else if( value instanceof YearMonth )
    {
      ps.setDate( pos, Date.valueOf( ((YearMonth)value).atDay( 1 ) ) );
    }
    else
    {
      ps.setObject( pos, value, getJdbcType() );
    }
  }
}
