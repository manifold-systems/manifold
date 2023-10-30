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

package manifold.rt.api;

import manifold.rt.api.util.ManObjectUtil;

import java.util.Map;

/**
 * Name/Value bindings
 */
public interface Bindings extends Map<String, Object>
{
  /**
   * Supports maintaining metadata about this instance
   */
  Bindings getMetadata();

  default String displayEntries()
  {
    StringBuilder row = new StringBuilder();
    for( Map.Entry<String, Object> entry : entrySet() )
    {
      if( row.length() > 0 )
      {
        row.append( ", " );
      }
      row.append( entry.getKey() ).append( ": " );
      Object value = entry.getValue();
      if( value instanceof String )
      {
        value = "\"" + value + "\"";
      }
      row.append( ManObjectUtil.toString( value ) );
    }
    return row.toString();
  }

  default String displayValues()
  {
    StringBuilder row = new StringBuilder();
    for( Object value : values() )
    {
      if( row.length() > 0 )
      {
        row.append( ", " );
      }
      if( value instanceof String )
      {
        value = "\"" + value + "\"";
      }
      row.append( ManObjectUtil.toString( value ) );
    }
    return row.toString();
  }
}