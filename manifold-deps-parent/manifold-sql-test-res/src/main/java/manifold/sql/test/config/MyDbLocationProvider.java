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

package manifold.sql.test.config;

import manifold.rt.api.util.StreamUtil;
import manifold.sql.rt.api.DbLocationProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MyDbLocationProvider implements DbLocationProvider
{
  @Override
  public String getLocation( Mode mode, String... args )
  {
    if( args.length < 1 )
    {
      throw new RuntimeException( "Expecting at least one argument" );
    }
    String dbFileResourcePath = args[0];
    String vendor = args.length > 1 ? args[1] : null;

    String tempDir = System.getProperty( "java.io.tmpdir" );
    if( tempDir.endsWith( "/" ) || tempDir.endsWith( "\\" ) )
    {
      tempDir = tempDir.substring( 0, tempDir.length() - 1 );
    }

    File tempDbFile = new File( tempDir + dbFileResourcePath );
    tempDbFile.getParentFile().mkdirs();
    tempDbFile.delete();
    try( InputStream in = getClass().getResourceAsStream( dbFileResourcePath );
         FileOutputStream out = new FileOutputStream( tempDir + dbFileResourcePath ) )
    {
      StreamUtil.copy( in, out );
      tempDbFile.deleteOnExit();

      String jdbcPath = tempDbFile.getAbsolutePath();
      if( vendor != null && vendor.equalsIgnoreCase( "h2" ) )
      {
        jdbcPath = jdbcPath.substring( 0, jdbcPath.toLowerCase().lastIndexOf( ".mv.db" ) );
      }
      return jdbcPath;
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }
}
