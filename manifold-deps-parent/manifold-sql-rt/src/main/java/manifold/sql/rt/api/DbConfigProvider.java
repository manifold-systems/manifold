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

public interface DbConfigProvider
{
  /**
   * This default implementation loads the file with base file name {@code configName} and extension {@code .dbconfig}
   * in the context of class {@code ctx}.
   * <p/>
   * The following locations are searched in order:<br>
   * <pre><code>
   * - ./<user.dir>/config <br>
   * - ./<user.dir> <br>
   * - ./<current-module-name>/<resource-path>/config <br>
   * - ./<resource-path>/config <br>
   * </code></pre>
   * @param configName The base name of the .dbconfig file.
   * @param ctx A class providing context, such as current module
   * @return An instance of {@link DbConfig} reflecting the settings in the .dbconfig file, or null if the file was not
   * found.
   */
  DbConfig loadDbConfig( String configName, Class<?> ctx );


  /**
   * Clears caching of DbConfig instances. For tests.
   */
  void clear();
}
