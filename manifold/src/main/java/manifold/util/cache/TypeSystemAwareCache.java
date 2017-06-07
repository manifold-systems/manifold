/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package manifold.util.cache;

import manifold.api.host.AbstractTypeSystemListener;
import manifold.api.host.RefreshRequest;
import manifold.internal.host.ManifoldHost;
import manifold.util.concurrent.Cache;

public class TypeSystemAwareCache<K, V> extends Cache<K, V>
{
  @SuppressWarnings({"FieldCanBeLocal"})
  private final AbstractTypeSystemListener _cacheClearer = new CacheClearer( this );

  public static <K, V> TypeSystemAwareCache<K, V> make( String name, int size, MissHandler<K, V> handler )
  {
    return new TypeSystemAwareCache<>( name, size, handler );
  }

  public TypeSystemAwareCache( String name, int size, MissHandler<K, V> kvMissHandler )
  {
    super( name, size, kvMissHandler );
    ManifoldHost.addTypeLoaderListenerAsWeakRef( null, _cacheClearer );
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
