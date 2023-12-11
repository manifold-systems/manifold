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

public interface Dependencies
{
  static Dependencies instance()
  {
    return DependenciesLookup.INSTANCE.get();
  }

  DbConfigProvider getDbConfigProvider();

  ConnectionProvider getConnectionProvider();

  CrudProvider getCrudProvider();

  DbLocationProvider getDbLocationProvider();

  DefaultTxScopeProvider getDefaultTxScopeProvider();

  TxScopeProvider getTxScopeProvider();

  TypeProvider getTypeProvider();

  ValueAccessorProvider getValueAccessorProvider();

  @SuppressWarnings( "unused" ) // used from generated code
  CustomEntityFactory getCustomEntityFactory();

  <T> T getOrCreate( Class<T> cls );
}
