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

package manifold.sql.rt.jdbc;

import manifold.sql.rt.api.TypeMap;
import manifold.sql.rt.api.RawElement;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.time.OffsetDateTime;

import static java.sql.Types.*;

public class DefaultJdbcTypeMap implements TypeMap
{
  @Override
  public Class<?> getType( RawElement column, int type )
  {
    switch( type )
    {
      case BIT:
      case BOOLEAN:
        return column.isNullable() ? Boolean.class : boolean.class;

      case TINYINT:
      case SMALLINT:
      case INTEGER:
        return column.isNullable() ? Integer.class : int.class;

      case BIGINT:
        return column.isNullable() ? Long.class : long.class;

      case REAL:
        return column.isNullable() ? Float.class : float.class;
        
      case FLOAT:
      case DOUBLE:
        return column.isNullable() ? Double.class : double.class;

      case NUMERIC:
      case DECIMAL:
        return BigDecimal.class;

      case CHAR:
      case VARCHAR:
      case LONGVARCHAR:
      case NCHAR:
      case NVARCHAR:
      case LONGNVARCHAR:
        return String.class;

      case DATALINK:
        return URL.class;
        
      case REF_CURSOR:
        return ResultSet.class;

      case SQLXML:
        return java.sql.SQLXML.class;

      case ROWID:
        return RowId.class;

      case DATE:
        return Date.class;
      case TIME:
        return Time.class;
      case TIMESTAMP:
        return Timestamp.class;

      case BINARY:
      case VARBINARY:
      case LONGVARBINARY:
        return byte[].class;

      case BLOB:
        return Blob.class;
      case CLOB:
        return Clob.class;
      case NCLOB:
        return NClob.class;

      case NULL:
      case JAVA_OBJECT:
      case DISTINCT:
      case OTHER:
        return Object.class;

      case STRUCT:
        return java.sql.Struct.class;

      case ARRAY:
        return Array.class;

      case REF:
        return Ref.class;

      case TIME_WITH_TIMEZONE:
        return java.time.OffsetTime.class;

      case TIMESTAMP_WITH_TIMEZONE:
        return OffsetDateTime.class;
    }
    throw new IllegalStateException( "Unknown type: " + type );
  }

  @Override
  public Priority getPriority()
  {
    return Priority.Low;
  }
}
