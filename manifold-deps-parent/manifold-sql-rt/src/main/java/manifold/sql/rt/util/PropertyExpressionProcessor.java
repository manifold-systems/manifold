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

import manifold.api.fs.IFile;
import manifold.api.util.cache.FqnCache;
import manifold.rt.api.util.Pair;
import manifold.sql.rt.api.DbLocationProvider;
import manifold.sql.rt.api.ExecutionEnv;
import manifold.sql.rt.api.Dependencies;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.Consumer;
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
  public static class Result
  {
    public String url;
    public List<Consumer<Connection>> initializers;

    public Result( String url, List<Consumer<Connection>> initializers )
    {
      this.url = url;
      this.initializers = initializers;
    }
  }

  public static Result process( Function<String, FqnCache<IFile>> resByExt, String source, ExecutionEnv executionEnv, Function<String, String> exprHandler )
  {
    if( source == null )
    {
      return null;
    }

    List<Consumer<Connection>> initializers = new ArrayList<>();

    for( int start = source.indexOf( "${" ); start >=0; start = source.indexOf( "${", start ) )
    {
      int end = source.indexOf( '}', start + 2 );
      if( end > 0 )
      {
        String expr = source.substring( start+2, end ).trim();
        Pair<String, Consumer<Connection>> value = eval( resByExt, expr, executionEnv, exprHandler );
        if( value.getFirst() != null )
        {
          source = new StringBuilder( source ).replace( start, end+1, value.getFirst() ).toString();
          if( value.getSecond() != null )
          {
            initializers.add( value.getSecond() );
          }
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
    return new Result( source, initializers );
  }

  private static Pair<String, Consumer<Connection>> eval( Function<String, FqnCache<IFile>> resByExt, String expr, ExecutionEnv executionEnv, Function<String, String> exprHandler )
  {
    Pair<String, Consumer<Connection>> result = evalProvided( resByExt, expr, executionEnv );
    if( result != null )
    {
      return result;
    }

    String value = exprHandler == null ? null : exprHandler.apply( expr );
    if( value == null )
    {
      value = System.getProperty( expr );
      if( value == null )
      {
        value = System.getenv( expr );
      }
    }
    value = makeSureTempDirEndsWithSeparator( expr, value );
    return new Pair<>( value, null );
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

  private static Pair<String, Consumer<Connection>> evalProvided( Function<String, FqnCache<IFile>> resByExt, String expr, ExecutionEnv executionEnv )
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
    DbLocationProvider dbLocProvider = Dependencies.instance().getDbLocationProvider();
    if( dbLocProvider != null )
    {
      Pair<Object, Consumer<Connection>> location = dbLocProvider.getLocation( resByExt, executionEnv, tag, argsArray );
      if( location.getFirst() != UNHANDLED )
      {
        return new Pair<>( String.valueOf( location.getFirst() ), location.getSecond() );
      }
    }
    return null;
  }
}
