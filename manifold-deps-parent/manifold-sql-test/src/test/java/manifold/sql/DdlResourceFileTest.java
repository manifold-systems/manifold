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

package manifold.sql;

import manifold.sql.rt.api.Dependencies;
import manifold.sql.rt.impl.DefaultTxScopeProvider;
import org.junit.After;

/**
 * Creates a fresh db file in a temp directory based on a DDL resource file, all connections, tx scopes, and DbConfigs
 * are cleared/closed after each test method. Effectively, each test method has a brand-new db as defined by the DDL
 * script.
 */
public abstract class DdlResourceFileTest
{
  @After
  public void cleanup()
  {
    // close and clear db connections
    Dependencies.instance().getConnectionProvider().closeAll();

    // clear default tx scopes
    DefaultTxScopeProvider.instance().clear();

    // clear DbConfigs. Forces dbconfig to initialize and exec ddl on first connection
    Dependencies.instance().getDbConfigProvider().clear();
  }
}
