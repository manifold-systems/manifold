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

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Implementors provide JDBC connections for all manifold-sql JDBC operations.
 */
public interface ConnectionProvider
{
  /**
   * Provides a JDBC connection corresponding with the {@code configName} {@link DbConfig} and optional {@code classContext}.
   * A standard implementation searches the {@code configName} DbConfig in the following order:
   * <li>Consult {@link DbConfigProvider} implementations</li>
   * <li>From {@code <configName>.dbconfig} file in the {@code /config} subdir of the current directory (runtime only)</li>
   * <li>From {@code <configName>.dbconfig} file in the current directory (runtime only)</li>
   * <li>From {@code <configName>.dbconfig} resource file in the {@code <module-name>.config} package (JDK 11+)</li>
   * <li>From {@code <configName>.dbconfig} resource file in the {@code config} package (JDK 8+)</li>
   * <p/>
   * This method is exclusive to <b>runtime</b> use.
   *
   * @param configName The name of the DbConfig. Does not include a file extension.
   * @param classContext The class initiating the connection. Used for context when searching for the DbConfig as a resource
   *                     file.
   * @return The JDBC connection
   */
  Connection getConnection( String configName, Class<?> classContext );

  /**
   * Provides a JDBC connection configured with the provided DbConfing.
   * <p/>
   * This method is exclusive to <b>build-time</b> use.
   *
   * @param dbConfig The configuration for the connection
   * @return The JDBC connection
   */
  Connection getConnection( DbConfig dbConfig ) throws SQLException;

  /**
   * Close the data source from which connections are made.
   * <p/>
   * This should be called only from IDE environments where the dbconfig can change while developing.
   *
   * @param dbconfig The DbConfig corresponding with the data source.
   */
  void closeDataSource( DbConfig dbconfig );

  /**
   * Close all resources.
   */
  void closeAll();
}
