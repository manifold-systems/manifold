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

package manifold.sql.rt.config;

import manifold.sql.rt.api.*;
import manifold.sql.rt.impl.accessors.DefaultValueAccessorProvider;
import manifold.sql.rt.impl.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultDependencies implements Dependencies
{
  private final Map<Class<?>, Object> _instances = new ConcurrentHashMap<>();

  @Override
  public DbConfigProvider getDbConfigProvider()
  {
    // loads config from .dbconfig file
    return fetch( DbConfigFinder.class );
  }

  @Override
  public ConnectionProvider getConnectionProvider()
  {
    return fetch( HikariConnectionProvider.class );
  }

  @Override
  public CrudProvider getCrudProvider()
  {
    return fetch( BasicCrudProvider.class );
  }

  @Override
  public DbLocationProvider getDbLocationProvider()
  {
    return fetch( ResourceDbLocationProvider.class );
  }

  @Override
  public TxScopeProvider getTxScopeProvider()
  {
    return fetch( BasicTxScopeProvider.class );
  }

  @Override
  public ValueAccessorProvider getValueAccessorProvider()
  {
    return fetch( DefaultValueAccessorProvider.class );
  }

  public <T> T fetch( Class<T> cls )
  {
    //noinspection unchecked
    return (T)_instances
      .computeIfAbsent( cls, __ -> {
        try
        {
          return cls.newInstance();
        }
        catch( Exception e )
        {
          throw new RuntimeException( e );
        }
      } );
  }
}
