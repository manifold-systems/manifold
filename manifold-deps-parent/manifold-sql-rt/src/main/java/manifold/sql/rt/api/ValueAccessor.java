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

package manifold.sql.rt.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * This interface performs the following:<br>
 * - provides Java types to be used for JDBC columns and parameters in Manifold's schema and SQL APIs
 * - sets query parameter values to JDBC<br>
 * - gets query result values from JDBC<br>
 * <br>
 */
public interface ValueAccessor
{
  Logger LOGGER = LoggerFactory.getLogger( ValueAccessor.class );

  /**
   * Indicates the JDBC type handled by this implementation.
   *
   * @return The {@link java.sql.Types} id this accessor handles. This type must be unique among other {@code ValueAccessor}
   * instances returned from {@link ValueAccessorProvider#get}.
   */
  int getJdbcType();

  /**
   * Provides the Java type to be used for JDBC columns and parameters in the Manifold SQL APIs.
   *
   * @param elem A schema column, query column, parameter, or other value bearing element.
   * @return The Java type to use for {@code elem} in the Manifold SQL APIs.
   */
  Class<?> getJavaType( BaseElement elem );

  /**
   * Returns a query result value corresponding with an {@code elem} from result set, {@code rs}.
   * @param rs The result set containing rows of column values
   * @param elem The query column containing the value
   * @return The value corresponding with {@code elem}. Note, the type of the value must match the Java type returned from
   * {@code elem.getJdbcType()}.
   * @throws SQLException
   */
  Object getRowValue( ResultSet rs, BaseElement elem ) throws SQLException;

  /**
   * Sets the query parameter value corresponding with {@code pos}.
   * @param ps The prepared statement containing the parameterized query.
   * @param pos The index of the parameter, beginning with 1.
   * @param value The value of the parameter
   * @throws SQLException
   */
  void setParameter( PreparedStatement ps, int pos, Object value ) throws SQLException;

  /**
   * Supply a parameter expression. Normally, this is just {@code ?}, however some JDBC drivers and database systems
   * require explicit casts when using certain data types. For instance, Postgres requires casts for several types e.g.,
   * Boolean to bit; there is no other way to get a value from Java into a Postgres bit column.
   */
  default String getParameterExpression( DatabaseMetaData metaData, Object value, ColumnInfo ci )
  {
    return "?";
  }

  /**
   * Use column class name
   */
  default Class<?> getClassForColumnClassName( String className, Class<?> defaultClass )
  {
    if( className != null && !className.equals( Object.class.getTypeName() ) )
    {
      try
      {
        return Class.forName( className );
      }
      catch( ClassNotFoundException cnfe )
      {
        LOGGER.warn( "Failed to access class '" + className + "' for '" + getClass().getSimpleName() + "'", cnfe );
      }
    }
    return defaultClass;
  }
}
