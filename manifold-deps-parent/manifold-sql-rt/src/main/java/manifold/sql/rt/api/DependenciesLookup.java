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
import manifold.sql.rt.config.DefaultDependencies;
import manifold.util.concurrent.LocklessLazyVar;

import java.util.LinkedHashSet;
import java.util.Set;

class DependenciesLookup
{
  private static final LocklessLazyVar<Set<Dependencies>> PROVIDERS =
    LocklessLazyVar.make( () -> {
      Set<Dependencies> registered = new LinkedHashSet<>();
      ServiceUtil.loadRegisteredServices( registered, Dependencies.class, Dependencies.class.getClassLoader() );
      return registered;
    } );

  static final LocklessLazyVar<Dependencies> INSTANCE =
    LocklessLazyVar.make( () -> {
      if( PROVIDERS.get().isEmpty() )
      {
        throw new RuntimeException( "Could not find Dependencies service provider" );
      }

      Dependencies result = null;
      for( Dependencies dependencies : PROVIDERS.get() )
      {
        // favor non-default dependencies
        if( result == null || result.getClass() == DefaultDependencies.class )
        {
          result = dependencies;
        }
      }
      return result;
    } );
}
