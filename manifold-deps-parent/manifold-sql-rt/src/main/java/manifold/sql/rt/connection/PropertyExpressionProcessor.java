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

package manifold.sql.rt.connection;

import manifold.sql.rt.api.DbLocationProvider;
import manifold.sql.rt.api.DbLocationProvider.Mode;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.Function;

import static manifold.sql.rt.api.DbLocationProvider.PROVIDED;

/**
 * Substitutes {@code ${<property-ref>}} expressions with corresponding values from system properties, environment vars,
 * and special references starting with {@code #}.
 * <p/>
 * Example: {@code ${java.io.tmpdir}/mydir/myfile.abc} -> {@code c:/tmp/mydir/myfile.abc}
 */
public class PropertyExpressionProcessor
{
  public static String process( String source, Mode mode, Function<String, String> exprHandler )
  {
    if( source == null )
    {
      return null;
    }

    for( int start = source.indexOf( "${" ); start >=0; start = source.indexOf( "${", start ) )
    {
      int end = source.indexOf( '}', start + 2 );
      if( end > 0 )
      {
        String expr = source.substring( start+2, end ).trim();
        String value = eval( expr, mode, exprHandler );
        if( value != null )
        {
          source = new StringBuilder( source ).replace( start, end+1, value ).toString();
        }
        else
        {
          throw new RuntimeException( "Expression '" + expr + "' in url : '" + source + "' " +
            "is not a valid system property or environment variable" );
        }
      }
      else
      {
        break;
      }
    }
    return source;
  }

  private static String eval( String expr, Mode mode, Function<String, String> exprHandler )
  {
    String value = evalProvided( expr, mode );
    if( value != null )
    {
      return value;
    }

    value = exprHandler == null ? null : exprHandler.apply( expr );
    if( value == null )
    {
      value = System.getProperty( expr );
      if( value == null )
      {
        value = System.getenv( expr );
      }
    }
    return value;
  }

  private static String evalProvided( String expr, Mode mode )
  {
    if( !expr.startsWith( PROVIDED + ' ' ) )
    {
      return null;
    }

    List<String> args = new ArrayList<>();
    String rest = expr.substring( PROVIDED.length() ).trim();
    for( StringTokenizer tokenizer = new StringTokenizer( rest, "," ); tokenizer.hasMoreTokens(); )
    {
      String arg = tokenizer.nextToken().trim();
      args.add( arg );
    }
    String[] argsArray = args.toArray( new String[0] );
    for( DbLocationProvider p : DbLocationProvider.PROVIDERS.get() )
    {
      String location = p.getLocation( mode, argsArray );
      if( location != null )
      {
        return location;
      }
    }
    return null;
  }
}
