/*
 * Copyright (c) 2018 - Manifold Systems LLC
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

package manifold.ext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import manifold.ext.api.ICoercionProvider;
import manifold.util.concurrent.LocklessLazyVar;

public class CoercionProviders
{
  private static final LocklessLazyVar<List<ICoercionProvider>> _coercionProviders =
    LocklessLazyVar.make( CoercionProviders::loadCoercionProviders );

  public static List<ICoercionProvider> get()
  {
    return _coercionProviders.get();
  }

  private static List<ICoercionProvider> loadCoercionProviders()
  {
    try
    {
      List<ICoercionProvider> providers = loadCoercionProviders( CoercionProviders.class.getClassLoader() );
      providers.addAll( loadCoercionProviders( Thread.currentThread().getContextClassLoader() ) );
      return providers;
    }
    catch( ServiceConfigurationError e )
    {
      e.printStackTrace();
    }
    return Collections.emptyList();
  }

  private static List<ICoercionProvider> loadCoercionProviders( ClassLoader cl )
  {
    List<ICoercionProvider> providers = new ArrayList<>();
    ServiceLoader<ICoercionProvider> loader = ServiceLoader.load( ICoercionProvider.class, cl );
    for( ICoercionProvider provider: loader )
    {
      providers.add( provider );
    }
    return providers;
  }
}