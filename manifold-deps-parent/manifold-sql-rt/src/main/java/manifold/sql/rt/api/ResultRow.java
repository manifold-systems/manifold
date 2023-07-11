/*
 * Copyright (c) 2020 - Manifold Systems LLC
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

package manifold.sql.rt.api;

import manifold.ext.rt.api.IBindingsBacked;
import manifold.rt.api.util.Pair;

/**
 * Base interface for all query result types type-safely reflecting query fields and structure.
 */
public interface ResultRow extends IBindingsBacked
{
  default String display()
  {
    StringBuilder row = new StringBuilder();
    for( Object value: getBindings().values() )
    {
      if( row.length() > 0 )
      {
        row.append( ", " );
      }
      if( value instanceof String )
      {
        value = "\"" + value + "\"";
      }
      row.append( value );
    }
    return row.toString();
  }

  default <T extends ResultTable> T fetchFk( Class<T> cls, String tableName, Pair<String,?>... values )
  {
    //todo: add a CRUD SPI and implement a "simple" one;  this method's impl is the 'R' (read) part: SELECT * FROM <tableName> WHERE values0 = ?, values1 = ?, etc.
    // note, the queries should be parameterized so the values can be passed in as parameter values using setObject()
    // note, maybe add a simple-ish cache for this that is also maintained for inserts, updates, and deletes
    return null;
  }
}
