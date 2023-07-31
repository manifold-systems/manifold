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

import manifold.api.fs.IFile;
import manifold.api.util.cache.FqnCache;
import manifold.sql.rt.api.DbLocationProvider;
import manifold.sql.rt.api.DbLocationProvider.Mode;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.Function;

import static manifold.sql.rt.api.DbLocationProvider.PROVIDED;
import static manifold.sql.rt.api.DbLocationProvider.UNHANDLED;

/**
 * Substitutes {@code ${<property-ref>}} expressions with corresponding values from system properties, environment vars,
 * and special references starting with {@code #} via custom {@link DbLocationProvider} implementations.<br>
 * <br>
 * Example: {@code ${java.io.tmpdir}/mydir/myfile.abc} -> {@code c:/tmp/mydir/myfile.abc}<br>
 * <br>
 * You can implement the {@link DbLocationProvider} SPI to handle {@code #<your-tag>} expressions:<br>
 * <br>
 * Example: {@code #mytag /mydir/myfile.abc, myinfo} -> {@code my url}
 */
public class PropertyExpressionProcessor
{
  public static String process( Function<String, FqnCache<IFile>> resByExt, String source, Mode mode, Function<String, String> exprHandler )
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
        String value = eval( resByExt, expr, mode, exprHandler );
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

  private static String eval( Function<String, FqnCache<IFile>> resByExt, String expr, Mode mode, Function<String, String> exprHandler )
  {
    String value = evalProvided( resByExt, expr, mode );
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
    value = makeSureTempDirEndsWithSeparator( expr, value );
    return value;
  }

  private static String makeSureTempDirEndsWithSeparator( String expr, String value )
  {
    // unix return "/tmp", but windows returns "/temp/", make this consistent wrt ending with separator

    if( expr.equals( "java.io.tmpdir" ) && value != null && !value.endsWith( "/" ) && !value.endsWith( "\\" ) )
    {
      value += "/";
    }
    return value;
  }

  private static String evalProvided( Function<String, FqnCache<IFile>> resByExt, String expr, Mode mode )
  {
    if( !expr.startsWith( PROVIDED ) )
    {
      return null;
    }

    int sp = expr.indexOf( ' ', 1 );
    if( sp < 0 )
    {
      return null;
    }

    String tag = expr.substring( 1, sp );

    List<String> args = new ArrayList<>();
    String rest = expr.substring( sp + 1 ).trim();
    for( StringTokenizer tokenizer = new StringTokenizer( rest, "," ); tokenizer.hasMoreTokens(); )
    {
      String arg = tokenizer.nextToken().trim();
      args.add( arg );
    }
    String[] argsArray = args.toArray( new String[0] );
    for( DbLocationProvider p : DbLocationProvider.PROVIDERS.get() )
    {
      Object location = p.getLocation( resByExt, mode, tag, argsArray );
      if( location != UNHANDLED )
      {
        return String.valueOf( location );
      }
    }
    return null;
  }
}
