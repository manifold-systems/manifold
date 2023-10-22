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

package manifold.sql.scratch;

import manifold.rt.api.util.StreamUtil;
import manifold.sql.rt.util.SqlScriptParser;
import manifold.util.ManExceptionUtil;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.*;
import java.util.List;

public class RunDdl
{

  public static void main( String args[] )
  {
    try
    {
      Class<?> aClass = Class.forName( "org.h2.Driver" );
      String path = "C:/manifold-systems/manifold/manifold-deps-parent/manifold-sql-inproc-test/src/test/resources/samples/db/h2-sakila.mv.db";
      File dbFile = getAndDeleteExistingFile( path );
      String urlName = removeMvDbExtension( dbFile );
      Connection c = DriverManager.getConnection( "jdbc:h2:file:" + urlName );
      System.out.println( "Database open successful" );

      ////////////////////////////////////////////////////////////////////////
      
      Statement stmt = c.createStatement();
      try( Reader reader = new InputStreamReader( RunDdl.class.getResourceAsStream( "/samples/ddl/h2-sakila-ddl.sql" ) ) )
      {
        String script = StreamUtil.getContent( reader );
        List<String> commands = SqlScriptParser.getCommands( script );
        for( String command : commands )
        {
          stmt.executeUpdate( command );
        }
        System.out.println( "Database creation successful" );
      }

      ////////////////////////////////////////////////////////////////////////

      stmt = c.createStatement();
      try( Reader reader = new InputStreamReader( RunDdl.class.getResourceAsStream( "/samples/ddl/h2-sakila-ddl.sql" ) ) )
      {
        String script = StreamUtil.getContent( reader );
        List<String> commands = SqlScriptParser.getCommands( script );
        for( String command : commands )
        {
          stmt.executeUpdate( command );
        }
        System.out.println( "Data load successful" );
      }

//      ResultSet resultSet = preparedStatement.executeQuery();
//      Object o;
//      o = resultSet.getObject( 1 );
//      o = resultSet.getObject( 4, String.class );
//      o = resultSet.getObject( 4, java.time.Instant.class );
//      c.getMetaData().getTypeInfo();
    }
    catch( Exception e )
    {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      throw ManExceptionUtil.unchecked( e );
    }
  }

  private static File getAndDeleteExistingFile( String path )
  {
    File dbFile = new File( path );
    dbFile.getParentFile().mkdirs();
    if( dbFile.isFile() )
    {
      dbFile.delete();
    }
    return dbFile;
  }

  private static String removeMvDbExtension( File dbFile )
  {
    String filename = dbFile.getName();
    filename = filename.substring( 0, filename.indexOf( '.' ) );
    String urlName = new File( dbFile.getParent(), filename ).getAbsolutePath().replace( '/', '/' );
    return urlName;
  }
}