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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class SqlScriptRunner
{
  /**
   * Runs a script of SQL commands. The commands are batched and executed in a single commit.
   *
   * @return An array of update counts containing one element for each command in the script.
   */
  public static int[] runScript( Connection connection, String script ) throws SQLException
  {
    boolean autoCommit = connection.getAutoCommit();
    try
    {
      connection.setAutoCommit( false );
      boolean goAsSeparator = connection.getMetaData().getDatabaseProductName().toLowerCase().contains( "sql server" );
      Statement stmt = connection.createStatement();
      List<String> commands = SqlScriptParser.getCommands( script, goAsSeparator );
      for( String command : commands )
      {
        stmt.addBatch( command );
      }
      int[] counts = stmt.executeBatch();
      connection.commit();
      return counts;
    }
    finally
    {
      connection.setAutoCommit( autoCommit );
    }
  }
}
