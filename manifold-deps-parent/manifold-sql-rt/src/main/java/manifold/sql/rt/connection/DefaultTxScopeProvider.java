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

package manifold.sql.rt.connection;

import manifold.sql.rt.api.SchemaType;
import manifold.sql.rt.api.TxScope;
import manifold.sql.rt.api.TxScopeProvider;

/**
 * Default scope is ThreadLocal per Schema type.
 */
public class DefaultTxScopeProvider
{
  private static DefaultTxScopeProvider INSTANCE;

  private final ThreadLocal<TxScope> _defaultScope = new ThreadLocal<>();

  private DefaultTxScopeProvider()
  {
  }

  public static DefaultTxScopeProvider instance()
  {
    return INSTANCE == null ? INSTANCE = new DefaultTxScopeProvider() : INSTANCE;
  }

  public TxScope defaultScope( Class<? extends SchemaType> schemaClass )
  {
    TxScope defaultScope = _defaultScope.get();
    if( defaultScope == null )
    {
      defaultScope = TxScopeProvider.newScope( schemaClass );
      _defaultScope.set( defaultScope );
    }
    return defaultScope;
  }

}
