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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sqlite is pretty much a mess regarding schema types reflecting result set types. The former is barely typed, lots of
 * strings etc., while the latter is more specific. However, since schema types correspond with schema column types, we have
 * to do better.
 * <p/>
 * Here is an attempt to improve the situation. The logic used here is taken from, ironically, the sqlite JDBC3ResultSet#getColumnType()
 * implementation, which unlike DatabaseMetadata#getColumns() + getString("DATA_TYPE"), gets it mostly right.
 *
 * todo: Utilize the new TypeProvider API to provide jdbc types in a more pluggable fashion, instead of bolted on like this
 */
public class SqliteTypeMapping
{
  private static final Pattern COLUMN_TYPENAME = Pattern.compile( "([^(]*)" );

  public String getProductName()
  {
    return "sqlite";
  }

  public Integer getJdbcType( String productName, ResultSet rs ) throws SQLException
  {
    if( !productName.equalsIgnoreCase( getProductName() ) )
    {
      return null;
    }

    // Use SQL type name to find JDBC type name, enter faint circus music
    String sqlType = rs.getString( "TYPE_NAME" );
    if( sqlType == null )
    {
      return null;
    }

    String baseName = extractBaseType( sqlType );
    sqlType = baseName == null ? sqlType : baseName;

    if( "BOOLEAN".equals( sqlType ) )
    {
      return Types.BOOLEAN;
    }

    if( "TINYINT".equals( sqlType ) )
    {
      return Types.TINYINT;
    }

    if( "SMALLINT".equals( sqlType ) || "INT2".equals( sqlType ) )
    {
      return Types.SMALLINT;
    }

    if( "BIGINT".equals( sqlType )
      || "INT8".equals( sqlType )
      || "UNSIGNED BIG INT".equals( sqlType ) )
    {
      return Types.BIGINT;
    }

    if( "DATE".equals( sqlType ) || "DATETIME".equals( sqlType ) )
    {
      return Types.DATE;
    }

    if( "TIMESTAMP".equals( sqlType ) )
    {
      return Types.TIMESTAMP;
    }

    if( "TIME".equals( sqlType ) )
    {
      return Types.TIME;
    }

    if( "INT".equals( sqlType )
      || "INTEGER".equals( sqlType )
      || "MEDIUMINT".equals( sqlType ) )
    {
      return Types.INTEGER;
    }

    if( "DECIMAL".equals( sqlType ) )
    {
      return Types.DECIMAL;
    }

    if( "DOUBLE".equals( sqlType ) || "DOUBLE PRECISION".equals( sqlType ) )
    {
      return Types.DOUBLE;
    }

    if( "NUMERIC".equals( sqlType ) )
    {
      return Types.NUMERIC;
    }

    if( "REAL".equals( sqlType ) )
    {
      return Types.REAL;
    }

    if( "FLOAT".equals( sqlType ) )
    {
      return Types.FLOAT;
    }

    if( "CHARACTER".equals( sqlType )
      || "NCHAR".equals( sqlType )
      || "NATIVE CHARACTER".equals( sqlType )
      || "CHAR".equals( sqlType ) )
    {
      return Types.CHAR;
    }

    if( "CLOB".equals( sqlType ) )
    {
      return Types.CLOB;
    }

    if( "VARCHAR".equals( sqlType )
      || "VARYING CHARACTER".equals( sqlType )
      || "NVARCHAR".equals( sqlType )
      || "TEXT".equals( sqlType ) )
    {
      return Types.VARCHAR;
    }

    if( "VARBINARY".equals( sqlType )
      || "VARYING BINARY".equals( sqlType ) )
    {
      return Types.VARBINARY;
    }

    if( "BINARY".equals( sqlType ) )
    {
      return Types.BINARY;
    }

    if( "BLOB".equals( sqlType ) )
    {
      return Types.BLOB;
    }

    if( "JAVA_OBJECT".equals( sqlType ) )
    {
      return Types.OTHER;
    }

    return null;
  }

  private static String extractBaseType( String sqlType )
  {
    Matcher matcher = COLUMN_TYPENAME.matcher( sqlType );
    return matcher.find()
      ? matcher.group( 1 ).toUpperCase( Locale.ENGLISH )
      : null;
  }
}
