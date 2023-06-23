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

package manifold.sql.schema;

import manifold.rt.api.util.StreamUtil;
import manifold.util.ManExceptionUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.*;

public class SQLiteJdbc_FromFile
{

  public static void main( String args[] )
  {
    try
    {
      File tempDbFile;
      try( InputStream stream = SQLiteJdbc_FromFile.class.getResourceAsStream( "/sakila/sqlite/sakila-sqlite.db" ) )
      {
        tempDbFile = File.createTempFile( "sakila-sqlite-", ".db" );
        try( OutputStream out = new FileOutputStream( tempDbFile ) )
        {
          StreamUtil.copy( stream, out );
        }
      }
      Connection c = DriverManager.getConnection( "jdbc:sqlite:" + tempDbFile.getAbsolutePath() );
      System.out.println( "Opened database successfully" );

      Statement stmt = c.createStatement();

      try( ResultSet columns = c.getMetaData().getColumns( null, null, "city", null ) )
      {
        while( columns.next() )
        {
          String columnName = columns.getString( "COLUMN_NAME" );
          String columnSize = columns.getString( "COLUMN_SIZE" );
          String datatype = columns.getString( "DATA_TYPE" );
          String isNullable = columns.getString( "IS_NULLABLE" );
          String isAutoIncrement = columns.getString( "IS_AUTOINCREMENT" );
        }
      }

      System.out.println( "Compiled:" );
      PreparedStatement preparedStatement = c.prepareStatement( "select * from city" );
      ResultSetMetaData rsMetaData = preparedStatement.getMetaData();
      for( int i = 1; i <= rsMetaData.getColumnCount(); i++ )
      {
        String columnClassName = rsMetaData.getColumnClassName( i );
        System.out.println( "columnClassName: " + columnClassName );
        String columnTypeName = rsMetaData.getColumnTypeName( i );
        System.out.println( "columnTypeName: " + columnTypeName );
        int type = rsMetaData.getColumnType( i );
        System.out.println( "columnType: " + type );
      }

      ResultSet resultSet = preparedStatement.executeQuery();
      Object o;
      System.out.println( o = resultSet.getObject( 1 ) );
      System.out.println( o = resultSet.getObject( 4, String.class ) );
      System.out.println( o = resultSet.getObject( 4, java.sql.Timestamp.class ) );
      c.getMetaData().getTypeInfo();
    }
    catch( Exception e )
    {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      throw ManExceptionUtil.unchecked( e );
    }
  }
}