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

package manifold.sql.rt.impl.accessors;

import manifold.sql.rt.api.TypeProvider;
import manifold.sql.rt.util.SqliteTypeMapping;

import java.sql.*;

public class DefaultTypeProvider implements TypeProvider
{
  @Override
  public int getSchemaColumnType( boolean isNonNullUniqueId, ResultSet rs, DatabaseMetaData metaData ) throws SQLException
  {
    int jdbcType = rs.getInt( "DATA_TYPE" );
    jdbcType = oneOffCorrections( jdbcType, rs, metaData );
    if( jdbcType == Types.NUMERIC )
    {
      jdbcType = findNumberType( jdbcType, rs.getInt( "COLUMN_SIZE" ), rs.getInt( "DECIMAL_DIGITS" ) );
      //jdbcType = oracleGreatness( isNonNullUniqueId, jdbcType );
    }
    return jdbcType;
  }

  //todo: this method is not in use right now because query parameter types remain BigDecimal and this change results in
  // the java Long type, which is much better, but clashes with BigDecimal whenever a query result column is passed as a
  // parameter, which happens a lot, and there's not much that can be done re the BigDecimal query param type, at least
  // not with Oracle :\
  private int oracleGreatness( boolean isNonNullUniqueId, int jdbcType )
  {
    if( isNonNullUniqueId && jdbcType == Types.NUMERIC )
    {
      // Some database schemas (oracle) often define pk columns as NUMBER, which maps to NUMERIC,
      // which maps to BigDecimal, which is not so good. BigDecimal is 99.99% not really the desired type.
      return Types.BIGINT; // java long type
    }
    return jdbcType;
  }

  @Override
  public int getQueryColumnType( int pos, ResultSetMetaData rm, DatabaseMetaData metaData ) throws SQLException
  {
    int jdbcType = rm.getColumnType( pos );
    if( jdbcType == Types.NUMERIC )
    {
      jdbcType = findNumberType( jdbcType, rm.getPrecision( pos ), rm.getScale( pos ) );
    }
    return jdbcType;
  }

  @Override
  public int getQueryParameterType( int pos, ParameterMetaData pm, DatabaseMetaData metaData ) throws SQLException
  {
    int jdbcType = pm.getParameterType( pos );

    // sqlite and mysql are known flakes here. See comment in exception below.
    String productName = metaData.getDatabaseProductName();
    if( jdbcType == Types.VARCHAR &&
      ("sqlite".equalsIgnoreCase( productName ) ||
        "mysql".equalsIgnoreCase( productName )) )
    {
      // the calling code handles this exception and provides default behavior for drivers with flaky parameter metadata
      throw new SQLException();
    }

    if( jdbcType == Types.NUMERIC )
    {
      jdbcType = findNumberType( jdbcType, pm.getPrecision( pos ), pm.getScale( pos ) );
    }
    return jdbcType;
  }

  /**
   * Some databases have only one integral type e.g., Oracle's NUMBER, that maps to jdbc NUMERIC type, which maps to Java's
   * BigDecimal. Here we generalize the mapping of NUMERIC types according to size.
   */
  private int findNumberType( int jdbcType, int size, int decimalDigits )
  {
    if( decimalDigits > 0 )
    {
      // not going to make assumptions re floating point types e.g., money should be BigDecimal regardless of scale/precision
      return jdbcType;
    }
    else
    {
      // attempt to infer a more appropriate integral type

      switch( size )
      {
        // although accepts all digits pos/neg 0-9, this is a standard mapping for boolean for oracle before 23c
        case 1:
          return Types.BOOLEAN;

        // popular size for TINYINT (esp. oracle's NUMBER)
        case 2:
          return Types.TINYINT; // Byte

        // popular range for SMALLINT (esp. oracle's NUMBER)
        case 3: case 4: case 5:
//        return Types.SMALLINT; // Short
          // Fall through to INTEGER because Short type requires casting of int values including integral literals.
          // As a consequence, short doesn't really provide much in terms of a statically enforceable constraint.

        // popular range for INTEGER (esp. oracle's NUMBER)
        case 6: case 7: case 8: case 9: case 10:
        return Types.INTEGER; // Integer

        // popular range for BIGINT
        // note sqlserver claims 18 is the size of NUMERIC, which is supposed to map to BigDecimal :\
        case 11: case 12: case 13: case 14: case 15: case 16: case 17:
        return Types.BIGINT; // Long

        // Otherwise, NUMERIC
        default:
          return jdbcType; // BigDecimal
      }
    }
  }

  private int oneOffCorrections( int jdbcType, ResultSet rs, DatabaseMetaData dm ) throws SQLException
  {
    String productName = dm.getDatabaseProductName();
    Integer newType = new SqliteTypeMapping().getJdbcType( productName, rs );
    return newType != null ? newType : jdbcType;
  }
}
