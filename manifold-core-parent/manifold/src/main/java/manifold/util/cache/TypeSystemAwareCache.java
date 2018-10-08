package manifold.util.cache;

import com.github.benmanes.caffeine.cache.CacheLoader;
import manifold.api.host.AbstractTypeSystemListener;
import manifold.api.host.IManifoldHost;
import manifold.api.host.RefreshRequest;
import manifold.util.concurrent.Cache;

public class TypeSystemAwareCache<K, V> extends Cache<K, V>
{
  @SuppressWarnings({"FieldCanBeLocal"})
  private final AbstractTypeSystemListener _cacheClearer = new CacheClearer( this );

  public static <K, V> TypeSystemAwareCache<K, V> make( IManifoldHost host, String name, int size, CacheLoader<K, V> loader )
  {
    return new TypeSystemAwareCache<>( host, name, size, loader );
  }

  public TypeSystemAwareCache( IManifoldHost host, String name, int size, CacheLoader<K, V> loader )
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
