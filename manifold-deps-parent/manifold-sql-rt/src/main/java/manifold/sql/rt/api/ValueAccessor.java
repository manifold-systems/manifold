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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The value returned from {@link #getJdbcType()} indicates the JDBC type handled by the implementation.
 * Manifold provides default implementations that are suitable for most use-cases.
 * <p/>
 * This interface performs the following:<br>
 * - resolves the Java type corresponding with the JDBC type from {@link java.sql.Types}<br>
 * - sets query parameter values<br>
 * - gets query result values<br>
 * <br>
 */
public interface ValueAccessor
{
  /**
   * @return The {@link java.sql.Types} id this accessor handles.
   */
  int getJdbcType();

  /**
   * @return The resulting type of the value in Java code. Note this type may not correspond with SQL-to-Java type mappings
   * from the JDBC specification. For instance, although {@code java.sql.Types#CLOB} maps to {@code java.sql.CLOB} (appendix
   * table B.3 from the JDBC 4.2 specification) the actual type generated for {@code CLOB} is {@code String}.
   */
  Class<?> getJavaType( BaseElement elem );

  /**
   * Returns a query result value corresponding with a {@code elem} from {@code rs}.
   * @param rs The result set containing rows of column values
   * @param elem The query column from which to find a value
   * @return The value corresponding with {@code elem}. Note, the type of the value must match the Java type returned from
   * {@code elem.getType()}.
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
}
