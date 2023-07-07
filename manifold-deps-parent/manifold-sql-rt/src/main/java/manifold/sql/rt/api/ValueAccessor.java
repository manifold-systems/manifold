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

import manifold.rt.api.util.ServiceUtil;
import manifold.util.concurrent.LocklessLazyVar;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Each implementation of this interface handles a JDBC type from {@link java.sql.Types} and must be registered as a Java
 * Service Provider. The value returned from {@link #getJdbcType()} indicates the JDBC type handled by the SPI implementation.
 * Manifold provides default implementations that are suitable for most use-cases, however these implementations can be
 * overridden by custom implementations, a higher priority from {@link #getPriority()} overrides lower priority implementations.
 * All Manifold implementations default to the lowest priority.
 * <p/>
 * This interface performs the following:<br>
 * - resolves the Java type corresponding with the JDBC type from {@link java.sql.Types}<br>
 * - sets query parameter values<br>
 * - gets query result values<br>
 * <br>
 */
public interface ValueAccessor
{
  LocklessLazyVar<Set<ValueAccessor>> ACCESSORS =
    LocklessLazyVar.make( () -> {
      Set<ValueAccessor> registered = new HashSet<>();
      ServiceUtil.loadRegisteredServices( registered, ValueAccessor.class, ValueAccessor.class.getClassLoader() );
      return registered;
    } );

  LocklessLazyVar<Map<Integer, ValueAccessor>> BY_JDBC_TYPE =
    LocklessLazyVar.make( () -> {
        Map<Integer, ValueAccessor> map = new HashMap<>();
        for( ValueAccessor acc : ACCESSORS.get() )
        {
          int jdbcType = acc.getJdbcType();
          ValueAccessor existing = map.get( jdbcType );
          if( existing == null || existing.getPriority() < acc.getPriority() )
          {
            map.put( jdbcType, acc );
          }
        }
        return map;
      } );

  static ValueAccessor get( int jdbcType )
  {
    return BY_JDBC_TYPE.get().get( jdbcType );
  }

  /**
   * @return The {@link java.sql.Types} id this accessor handles.
   */
  int getJdbcType();

  /**
   * Greater = higher priority. Higher priority overrides lower. Default implementations are lowest priority. They can be
   * overridden.
   */
  default int getPriority()
  {
    return Integer.MIN_VALUE;
  }

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
