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

public class CommonJDBC
{
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