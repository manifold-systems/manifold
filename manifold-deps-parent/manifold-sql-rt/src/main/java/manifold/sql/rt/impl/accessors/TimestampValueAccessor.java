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
import java.time.Instant;
import java.time.LocalDateTime;

public class TimestampValueAccessor implements ValueAccessor
{
  @Override
  public int getJdbcType()
  {
    return Types.TIMESTAMP;
  }

  @Override
  public Class<?> getJavaType( BaseElement elem )
  {
    return LocalDateTime.class;
  }

  @Override
  public LocalDateTime getRowValue( ResultSet rs, BaseElement elem ) throws SQLException
  {
    Timestamp timestamp = rs.getTimestamp( elem.getPosition() );
    return timestamp == null ? null : timestamp.toLocalDateTime();
  }

  @Override
  public void setParameter( PreparedStatement ps, int pos, Object value ) throws SQLException
  {
    if( value == null )
    {
      ps.setNull( pos, getJdbcType() );
    }
    else if( value instanceof LocalDateTime )
    {
      ps.setTimestamp( pos, Timestamp.valueOf( (LocalDateTime)value ) );
    }
    else if( value instanceof Instant )
    {
      ps.setTimestamp( pos, Timestamp.from( (Instant)value ) );
    }
    else
    {
      ps.setObject( pos, value, getJdbcType() );
    }
  }
}
