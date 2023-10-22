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

package manifold.sql.rt.impl;

import manifold.api.fs.IFile;
import manifold.api.util.cache.FqnCache;
import manifold.rt.api.util.Pair;
import manifold.rt.api.util.StreamUtil;
import manifold.rt.api.util.TempFileUtil;
import manifold.sql.rt.api.DbLocationProvider;
import manifold.sql.rt.util.SqlScriptRunner;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static manifold.rt.api.util.TempFileUtil.makeTempFile;
import static manifold.sql.rt.api.DbLocationProvider.Mode.*;

/**
 * DbLocationProvider for: {@code #resource} and {@code #resource_script}. This functionality is primarily aimed at testing
 * to support a fresh database file for embeddable databases such as H2, Sqlite, etc.
 * <p/>
 * Handle expression syntax like: {@code ${#resource /org/example/MyDatabase.db [, MyName][, args]}}
 * <p/>
 * Where {@code /org/example/MyDatabase.db} is an absolute Java resource path to a sql database file and the optional
 * {@code MyName} parameter is the file name to be used for the resulting URL path string (if different from the file name),
 * and {@code args} can be additional options such as {@code ";create=true;blah=blah"}. Alternatively, additional options
 * can usually be provided as DbConfig properties.
 * <p/>
 * Copies the contents of {@code /org/example/MyDatabase.db} to a temp directory and returns the temp dir path, and if
 * specified, replaces {@code MyDatabase.db} file name with {@code MyName}.
 * <p/>
 * Also handles {@code ${#resource_script ... }} with same functionality as {@code #resource}, but the resource path is
 * a DDL script instead of a database file. Here, {@code MyName} must be provided as the resulting URL db file name. The
 * DDL file is executed when the first connection is made to the db.
 * <p/>
 */
public class ResourceDbLocationProvider implements DbLocationProvider
{
  public static final String RESOURCE = "resource";
  public static final String RESOURCE_SCRIPT = "resource_script";

  @Override
  public Pair<Object, Consumer<Connection>> getLocation( Function<String, FqnCache<IFile>> resByExt, Mode mode, String tag, String... args )
  {
    if( tag.equals( RESOURCE ) )
    {
      return resource( false, resByExt, mode, args );
    }
    else if( tag.equals( RESOURCE_SCRIPT ) )
    {
      return resourceScript( resByExt, mode, args );
    }

    return new Pair<>( UNHANDLED, null );
  }

  private Pair<Object, Consumer<Connection>> resourceScript( Function<String, FqnCache<IFile>> resByExt, Mode mode, String[] args )
  {
    if( args.length < 2 )
    {
      throw new RuntimeException( "Expecting at least two arguments: <resource path>, <file name>" );
    }

    return resource( true, resByExt, mode, args );
  }

  private Pair<Object, Consumer<Connection>> resource( boolean isScript, Function<String, FqnCache<IFile>> resByExt, Mode mode, String[] args )
  {
    if( args.length < 1 )
    {
      throw new RuntimeException( "Expecting at least one argument" );
    }

    String dbFileResourcePath = args[0];
    String name = args.length > 1 ? args[1] : null;
    String extra = args.length > 2 ? args[2] : null;

    // add prefix to temp file to prevent compile-time, design-time, and run-time usages collide,
    // each will have its own copy of the db
    String prefix = "/" + mode;
    if( !dbFileResourcePath.startsWith( "/" ) && !dbFileResourcePath.startsWith( File.separator ) )
    {
      dbFileResourcePath = "/" + dbFileResourcePath;
    }

    String tempDir = makeTempDirName( dbFileResourcePath );
    deleteTempDbDir( mode, dbFileResourcePath );
    File tempDbFile = TempFileUtil.makeTempFile( "/" + prefix + "/" + tempDir + dbFileResourcePath, true );
    IFile resFile = maybeGetCompileTimeResource( resByExt, mode, dbFileResourcePath );
    try( InputStream in = resFile != null
      ? resFile.openInputStream()
      : getClass().getResourceAsStream( dbFileResourcePath );
         FileOutputStream out = new FileOutputStream( tempDbFile ) )
    {
      if( in == null )
      {
        throw new IOException( "Db resource '" + dbFileResourcePath + "' was not found" );
      }

      Consumer<Connection> init = null;
      if( isScript )
      {
        String script = new String( StreamUtil.getContent( in ) );
        init = new Initializer( script );
      }
      else
      {
        StreamUtil.copy( in, out );
      }

      String jdbcPath = tempDbFile.getAbsolutePath();
      if( name != null )
      {
        File f = new File( tempDbFile.getParent(), name );
        jdbcPath = f.getAbsolutePath().replace( '\\', '/' );
      }
      if( extra != null )
      {
        jdbcPath += extra;
      }
      return new Pair<>( jdbcPath, init );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  @NotNull
  public static String makeTempDirName( String dbFileResourcePath )
  {
    String tempDir;
    int slashPos = dbFileResourcePath.lastIndexOf( "/" );
    if( slashPos < 0 )
    {
      slashPos = dbFileResourcePath.lastIndexOf( "\\" );
    }

    if( slashPos >= 0 )
    {
      tempDir = dbFileResourcePath.substring( slashPos + 1 );
    }
    else
    {
      tempDir = dbFileResourcePath;
    }
    return tempDir;
  }

  public static void deleteTempDbDir( Mode mode, String dbFileResourcePath )
  {
    // delete temp db directory
    File file = makeTempFile( "/" + mode + "/" + makeTempDirName( dbFileResourcePath ) );
    if( !file.exists() )
    {
      return;
    }
    
    try( Stream<Path> pathStream = Files.walk( file.toPath() ) )
    {
      //noinspection ResultOfMethodCallIgnored
      pathStream.sorted( Comparator.reverseOrder() )
        .map( Path::toFile )
        .forEach( File::delete );

      // wait for file to delete from the fs, to prevent tests from clobbering one another
      long now = System.nanoTime();
      Path path = Paths.get( file.toURI() );
      while( !Files.notExists( path ) )
      {
        if( System.nanoTime() - now > 10 * 1_000_000_000L )
        {
          // bail after 10s
          throw new RuntimeException( "Could not delete temp db directory: " + path );
        }
        //noinspection BusyWait
        Thread.sleep( 100 );
      }
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  public static IFile maybeGetCompileTimeResource( Function<String, FqnCache<IFile>> resByExt, Mode mode, String dbFileResourcePath )
  {
    if( mode == CompileTime || mode == DesignTime )
    {
      IFile[] resFile = {null};
      int idot = dbFileResourcePath.lastIndexOf( '.' );
      String ext;
      if( idot > 0 )
      {
        ext = dbFileResourcePath.substring( idot + 1 );
      }
      else
      {
        throw new RuntimeException( "Expecting a file extension: " + dbFileResourcePath );
      }

      FqnCache<IFile> extensionCache = resByExt.apply( ext );
      extensionCache.visitDepthFirst(
        file ->
        {
          if( file != null && file.getPath().getPathString( "/" ).endsWith( dbFileResourcePath ) )
          {
            resFile[0] = file;
            return false;
          }
          return true;
        } );
      return resFile[0];
    }
    return null;
  }

  private static class Initializer implements Consumer<Connection>
  {
    private final String _script;
    private boolean _initialized;

    public Initializer( String script )
    {
      _script = script;
    }

    @Override
    public void accept( Connection connection )
    {
      if( _initialized )
      {
        return;
      }
      try
      {
        SqlScriptRunner.runScript( connection, _script );
        _initialized = true;
      }
      catch( SQLException e )
      {
        throw new RuntimeException( e );
      }
    }
  }
}
