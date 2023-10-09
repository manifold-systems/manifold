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

package manifold.sql.rt.util;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public class DbUtil
{
  public static String enquoteIdentifier( String id, DatabaseMetaData metaData ) throws SQLException
  {
    // "foo" is a standard SQL identifier (column/table/etc)
    // [foo] is a (olden times) Sql Server identifier
    // `foo` is a MySql identifier
    // ...

    String quoteStr = metaData.getIdentifierQuoteString();
    if( quoteStr.equals( " " ) )
    {
      // db does not support quoted identifiers
      return id;
    }

    if( quoteStr.length() == 1 )
    {
      return quoteStr + id + quoteStr;
    }
    if( quoteStr.length() == 2 )
    {
      return quoteStr.charAt( 0 ) + id + quoteStr.charAt( 1 );
    }
    throw new SQLException( "Unexpected identifier quote string: " + quoteStr );
  }

  public static final String handleAnonQueryColumn( String name, int oneBasedIndex )
  {
    if( name == null || name.isEmpty() )
    {
      // some drivers return empty string for calculated columns instead of generating something (sql server) :\
      name = "col_" + oneBasedIndex;
    }
    return name;
  }
}
