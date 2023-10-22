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

import manifold.util.ManExceptionUtil;

import java.lang.reflect.Field;
import java.sql.*;

public class H2JDBC
{

  public static void main( String args[] )
  {
    try
    {
//      Class<?> aClass = Class.forName( "org.h2.Driver" );
//      System.out.println(aClass);
//      Connection c = DriverManager.getConnection( "jdbc:sqlite::memory:" );
      Connection c = DriverManager.getConnection( "jdbc:h2:file:C:/manifold-systems/manifold/manifold-deps-parent/manifold-sql-inproc-test/src/test/Sales" );
      System.out.println( "Opened database successfully" );

//      PreparedStatement ps = c.prepareStatement( "select * from product" );
//      ResultSet resultSet = ps.executeQuery();
//      while( resultSet.next() )
//      {
//        System.out.print( resultSet.getObject( 1 ) + ", " );
//        System.out.print( resultSet.getObject( 2 ) + ", " );
//        System.out.print( resultSet.getObject( 3 ) + ", " );
//        System.out.print( resultSet.getObject( 4 ) + "\n" );
//      }
//      resultSet.close();

      DatabaseMetaData metaData = c.getMetaData();

      printAllDataTypes( metaData );

      try( ResultSet resultSet = metaData.getTables( null, "PUBLIC", null, new String[]{"TABLE", "VIEW"} ); )
      {
        while( resultSet.next() )
        {
          String schema = resultSet.getString( "TABLE_SCHEM" );
          if( schema != null && schema.equalsIgnoreCase( "information_schema" ) )
          {
            // skip information_schema
            continue;
          }
          String name = resultSet.getString( "TABLE_NAME" );
          System.out.println( "//////////////////////////////////////////////////");
          System.out.println( name );
          System.out.println("///////////////////////////////////////////////////");
          try( ResultSet columns = metaData.getColumns( null, null, name, null ) )
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
//      Statement stmt = c.createStatement();
//      try( Reader reader = new InputStreamReader( H2JDBC.class.getResourceAsStream( "/samples/h2/h2-sales-ddl.sql" ) ) )
//      {
//        String script = StreamUtil.getContent( reader );
//        List<String> commands = SqlScriptParser.getCommands( script );
//        for( String command : commands )
//        {
//          stmt.executeUpdate( command );
//        }
//        System.out.println( "Operation done successfully" );
//      }
//      try(ResultSet columns = c.getMetaData().getColumns(null,null, "product", null)){
//        while(columns.next()) {
//          String columnName = columns.getString("COLUMN_NAME");
//          String columnSize = columns.getString("COLUMN_SIZE");
//          String datatype = columns.getString("DATA_TYPE");
//          String isNullable = columns.getString("IS_NULLABLE");
//          String isAutoIncrement = columns.getString("IS_AUTOINCREMENT");
//        }
//      }
//
//      System.out.println("Compiled:");
//      PreparedStatement preparedStatement = c.prepareStatement( "select * from product" );
//      ResultSetMetaData rsMetaData = preparedStatement.getMetaData();
//      for( int i = 1; i <= rsMetaData.getColumnCount(); i++ )
//      {
//        String columnClassName = rsMetaData.getColumnClassName( i );
//        System.out.println( "columnClassName: " + columnClassName );
//        String columnTypeName = rsMetaData.getColumnTypeName( i );
//        System.out.println( "columnTypeName: " + columnTypeName );
//        int type = rsMetaData.getColumnType( i );
//        System.out.println( "columnType: " + type );
//      }
//
////      ResultSet resultSet = preparedStatement.executeQuery();
////      Object o;
////      o = resultSet.getObject( 1 );
////      o = resultSet.getObject( 4, String.class );
////      o = resultSet.getObject( 4, java.time.Instant.class );
////      c.getMetaData().getTypeInfo();
    }
    catch( Exception e )
    {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      throw ManExceptionUtil.unchecked( e );
    }
  }

  public static void printAllDataTypes( DatabaseMetaData metaData ) throws SQLException, IllegalAccessException
  {
    try( ResultSet typeInfo = metaData.getTypeInfo() )
    {
      StringBuilder createTable = new StringBuilder( "create table all_types (\n");
      int i = 0;
      while( typeInfo.next() )
      {
        i++;
        String typeName = typeInfo.getString( "TYPE_NAME" );
        int dataType = typeInfo.getInt( "DATA_TYPE" );
        String jdbcDataType = null;
        for( Field f : Types.class.getFields() )
        {
          int value = (int)f.get( null );
          if( dataType == value )
          {
            jdbcDataType = f.getName();
          }
        }
        if( typeName.equalsIgnoreCase( "number" ) )
        {
          // note, oracle only has one type for all numeric/boolean types: NUMBER
          
          int precision = typeInfo.getInt( "PRECISION" );
          typeName += "(" + precision;
          int scale = typeInfo.getInt( "MAXIMUM_SCALE" );
          if( scale < Byte.MAX_VALUE )
          {
            typeName += "," + scale;
          }
          typeName += ")";
        }
        createTable.append( String.format( "    col_%-2d   %-30s -- $jdbcDataType($dataType)\n", i, typeName ) );
        System.out.println( typeName + " :           " + jdbcDataType + "(" + dataType + ")" );
      }
      createTable.append( ");\n" );
      System.out.println( createTable );
      
    }
  }
}