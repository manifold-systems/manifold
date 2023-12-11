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

import manifold.rt.api.util.StreamUtil;
import manifold.sql.rt.api.ConnectionProvider;
import manifold.sql.rt.api.DbConfig;
import manifold.sql.rt.api.Dependencies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.function.BiPredicate;

import static manifold.sql.rt.util.DriverInfo.Oracle;
import static manifold.sql.rt.util.DriverInfo.SqlServer;

public class SqlScriptRunner
{
  private static final Logger LOGGER = LoggerFactory.getLogger( SqlScriptRunner.class );

  /**
   * Runs a script of SQL commands. The commands are batched and executed in a single commit.
   *
   * @return An array of update counts containing one element for each command in the script.
   */
  public static void runScript( Connection connection, String script ) throws SQLException
  {
    runScript( connection, script, null );
  }
  /**
   * @param exceptionHandler allows one to ignore a failed command and continue executing remaining commands.
   *                         Note, providing an exceptionHandler changes the execution of commands from a batch operation
   *                         to a series of individual statement executions.
   */
  public static void runScript( Connection connection, String script, BiPredicate<String, SQLException> exceptionHandler ) throws SQLException
  {
    boolean autoCommit = connection.getAutoCommit();
    try
    {
      connection.setAutoCommit( false );
      Statement stmt = connection.createStatement();
      List<String> commands = SqlScriptParser.getCommands( script, extraSeparator( connection ) );
      for( String command : commands )
      {
        if( exceptionHandler == null )
        {
          stmt.addBatch( command );
        }
        else
        {
          try
          {
            stmt.execute( command );
          }
          catch( SQLException e )
          {
            if( !exceptionHandler.test( command, e ) )
            {
              throw e;
            }
          }
        }
      }
      if( exceptionHandler == null )
      {
        stmt.executeBatch();
      }
      connection.commit();
    }
    finally
    {
      connection.setAutoCommit( autoCommit );
    }
  }

  public static void runScript( String dataResourcePath, DbConfig dbconfig, Class<?> ctxClass) throws IOException
  {
    InputStream is = ctxClass.getResourceAsStream( dataResourcePath );
    if( is == null )
    {
      throw new IOException( "Could not find resource file: '" + dataResourcePath + "'" );
    }
    try( Reader reader = new InputStreamReader( is ) )
    {
      ConnectionProvider cp = Dependencies.instance().getConnectionProvider();
      try( Connection c = cp.getConnection( dbconfig.getName(), ctxClass ) )
      {
        String script = StreamUtil.getContent( reader );
        runScript( c, script );
      }
      catch( SQLException e )
      {
        throw new RuntimeException( e );
      }
      LOGGER.info( "SQL script run successful: '" + dataResourcePath + "'" );
    }
  }

  private static SqlScriptParser.ExtraSeparator extraSeparator( Connection c ) throws SQLException
  {
    DriverInfo driver = DriverInfo.lookup( c.getMetaData() );
    if( driver == SqlServer )
    {
      return SqlScriptParser.ExtraSeparator.Go;
    }
    if( driver == Oracle )
    {
      return SqlScriptParser.ExtraSeparator.Slash;
    }
    return null;
  }
}
