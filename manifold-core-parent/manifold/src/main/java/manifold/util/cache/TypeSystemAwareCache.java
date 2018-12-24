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

package manifold.util.cache;

import manifold.api.host.AbstractTypeSystemListener;
import manifold.api.host.IManifoldHost;
import manifold.api.host.RefreshRequest;
import manifold.util.concurrent.Cache;

public class TypeSystemAwareCache<K, V> extends Cache<K, V>
{
  @SuppressWarnings({"FieldCanBeLocal"})
  private final AbstractTypeSystemListener _cacheClearer = new CacheClearer( this );

  public static <K, V> TypeSystemAwareCache<K, V> make( IManifoldHost host, String name, int size, Loader<K, V> loader )
  {
    return new TypeSystemAwareCache<>( host, name, size, loader );
  }

  public TypeSystemAwareCache( IManifoldHost host, String name, int size, Loader<K, V> loader )
  {
    super( name, size, loader );
    host.addTypeSystemListenerAsWeakRef( null, _cacheClearer );
  }

  private static class CacheClearer extends AbstractTypeSystemListener
  {
    TypeSystemAwareCache _cache;

    private CacheClearer( TypeSystemAwareCache cache )
    {
      _cache = cache;
    }

    @Override
    public void refreshed()
    {
      _cache.clear();
    }

    @Override
    public void refreshedTypes( RefreshRequest request )
    {
      _cache.clear();
    }

  }
}
