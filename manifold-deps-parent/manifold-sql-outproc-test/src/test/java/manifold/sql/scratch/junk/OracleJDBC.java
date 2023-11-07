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

package manifold.sql.scratch.junk;

import manifold.json.rt.Json;
import manifold.rt.api.Bindings;
import manifold.rt.api.util.StreamUtil;
import manifold.sql.rt.api.*;
import manifold.sql.rt.impl.DbConfigImpl;
import manifold.util.ManExceptionUtil;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

public class OracleJDBC extends CommonJDBC
{

  public static void main( String[] args )
  {
//    scratchStuff();
    printAllTypes();
  }

  private static void printAllTypes()
  {

    try
    {
      ConnectionProvider cp = Dependencies.instance().getConnectionProvider();
      Connection c = cp.getConnection( makeDbConfig() );

//      Connection c = DriverManager.getConnection( "jdbc:oracle:thin:@localhost:1521:XE?user=system&password=password" );
      System.out.println( "Opened database successfully" );

      DatabaseMetaData metaData = c.getMetaData();

      printAllDataTypes( metaData );

      try( ResultSet resultSet = metaData.getTables( null, "SAKILA", null, new String[]{"TABLE", "VIEW"} ); )
      {
        while( resultSet.next() )
        {
          String schema = resultSet.getString( "TABLE_SCHEM" );
          if( schema == null || !schema.equalsIgnoreCase( "sakila" ) )
          {
            // skip information_schema
            continue;
          }
          String name = resultSet.getString( "TABLE_NAME" );
          System.out.println( "//////////////////////////////////////////////////");
          System.out.println( name );
          System.out.println("///////////////////////////////////////////////////");
          try( ResultSet columns = metaData.getColumns( null, "SAKILA", name, null ) )
          {
            while( columns.next() )
            {
              String columnName = columns.getString( "COLUMN_NAME" );
              System.out.print( columnName + ", " );
              String columnSize = columns.getString( "COLUMN_SIZE" );
              System.out.print( columnSize + ", " );
              String datatype = columns.getString( "DATA_TYPE" );
              System.out.print( datatype + ", " );
              String isNullable = columns.getString( "IS_NULLABLE" );
              System.out.print( isNullable + ", " );
              String isAutoIncrement = columns.getString( "IS_AUTOINCREMENT" );
              System.out.print( isAutoIncrement + "\n" );
            }
          }
        }
      }

    }
    catch( Exception e )
    {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      throw ManExceptionUtil.unchecked( e );
    }
  }

  static DbConfig makeDbConfig()
  {
    Bindings bindings = null;
    try
    {
      bindings = (Bindings)Json.fromJson( StreamUtil.getContent( new InputStreamReader( OracleJDBC.class.getResourceAsStream( "/config/OracleSakila.dbconfig" ) ) ) );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
    bindings.put( "name", "OracleSakila" );
    return new DbConfigImpl( bindings, ExecutionEnv.Compiler );
  }
}