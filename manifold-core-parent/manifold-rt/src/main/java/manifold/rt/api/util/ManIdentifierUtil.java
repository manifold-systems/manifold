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

package manifold.rt.api.util;

public class ManIdentifierUtil
{
  public static String makeIdentifier( String name )
  {
    String identifier = ReservedWordMapping.getIdentifierForName( name );
    if( !identifier.equals( name ) )
    {
      return identifier;
    }

    StringBuilder sb = new StringBuilder();
    for( int i = 0; i < name.length(); i++ )
    {
      char c = name.charAt( i );
      if( sb.length() == 0 && Character.isWhitespace( c ) )
      {
        // skip leading whitespace
        continue;
      }

      if( i == 0 && c >= '0' && c <= '9' )
      {
        sb.append( '_' ).append( c );
      }
      else if( c == '_' ||
               c == '$' ||
               c >= 'A' && c <= 'Z' ||
               c >= 'a' && c <= 'z' ||
               c >= '0' && c <= '9' )
      {
        sb.append( c );
      }
      else
      {
        sb.append( '_' );
      }
    }
    identifier = makeCorrections( sb );
    return identifier;
  }

  private static String makeCorrections( StringBuilder sb )
  {
    String identifier = sb.toString();
    if( isAllUnderscores( identifier ) )
    {
      identifier = "_" + identifier.length();
    }
    return identifier;
  }

  private static boolean isAllUnderscores( String result )
  {
    for( int i = 0; i < result.length(); i++ )
    {
      if( result.charAt( i ) != '_' )
      {
        return false;
      }
    }
    return true;
  }
}
