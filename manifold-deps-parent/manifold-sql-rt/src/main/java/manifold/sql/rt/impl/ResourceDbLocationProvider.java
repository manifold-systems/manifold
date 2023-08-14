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
import manifold.rt.api.util.StreamUtil;
import manifold.rt.api.util.TempFileUtil;
import manifold.sql.rt.api.DbLocationProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

import static manifold.sql.rt.api.DbLocationProvider.Mode.*;

/**
 * Handle expression syntax like: {@code ${#resource /org/example/MyDatabase.db [, MyName]}}<br>
 * Where {@code /org/example/MyDatabase.db} is an absolute Java resource path and the optional {@code MyName} is the file
 * name to be used for the resulting URL path string.
 * <p/>
 * Copies the contents of {@code /org/example/MyDatabase.db} to the temp directory and returns the path of the temporary
 * file, in this case replacing {@code MyDatabase.db} with {@code MyName} as the file name.
 * <p/>
 */
public class ResourceDbLocationProvider implements DbLocationProvider
{
  public static final String RESOURCE = "resource";

  @Override
  public Object getLocation( Function<String, FqnCache<IFile>> resByExt, Mode mode, String tag, String... args )
  {
    if( tag.equals( RESOURCE ) )
    {
      return resource( resByExt, mode, args );
    }

    return UNHANDLED;
  }

  private Object resource( Function<String, FqnCache<IFile>> resByExt, Mode mode, String[] args )
  {
    if( args.length < 1 )
    {
      throw new RuntimeException( "Expecting at least one argument" );
    }

    String dbFileResourcePath = args[0];
    String name = args.length > 1 ? args[1] : null;

    // add prefix to temp file to prevent compile-time, design-time, and run-time usages collide,
    // each will have its own copy of the db
    String prefix = "/" + mode;
    if( !dbFileResourcePath.startsWith( "/" ) && !dbFileResourcePath.startsWith( File.separator ) )
    {
      dbFileResourcePath = "/" + dbFileResourcePath;
    }
    File tempDbFile = TempFileUtil.makeTempFile( prefix + dbFileResourcePath, true );
    //noinspection ResultOfMethodCallIgnored
    tempDbFile.delete();
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

      StreamUtil.copy( in, out );

      String jdbcPath = tempDbFile.getAbsolutePath();
      if( name != null )
      {
        File f = new File( tempDbFile.getParent(), name );
        jdbcPath = f.getAbsolutePath().replace( '\\', '/' );
      }
      return jdbcPath;
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  private static IFile maybeGetCompileTimeResource( Function<String, FqnCache<IFile>> resByExt, Mode mode, String dbFileResourcePath )
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
}
