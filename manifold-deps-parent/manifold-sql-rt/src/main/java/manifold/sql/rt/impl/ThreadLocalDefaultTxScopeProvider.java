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

package manifold.sql.rt.impl;

import manifold.sql.rt.api.Dependencies;
import manifold.sql.rt.api.SchemaType;
import manifold.sql.rt.api.TxScope;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default scope is ThreadLocal per Schema type.
 */
public class ThreadLocalDefaultTxScopeProvider implements manifold.sql.rt.api.DefaultTxScopeProvider
{
  private final ThreadLocal<Map<Class<? extends SchemaType>, TxScope>> _defaultScopes =
    ThreadLocal.withInitial( () -> new LinkedHashMap<>() );

  public TxScope defaultScope( Class<? extends SchemaType> schemaClass )
  {
    Map<Class<? extends SchemaType>, TxScope> defaultScopes = _defaultScopes.get();
    TxScope defaultScope = defaultScopes.get( schemaClass );
    if( defaultScope == null )
    {
      defaultScope = Dependencies.instance().getTxScopeProvider().newScope( schemaClass );
      defaultScopes.put( schemaClass, defaultScope );
    }
    return defaultScope;
  }

  public void clear()
  {
    _defaultScopes.remove();
  }
}
