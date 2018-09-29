package manifold.util.cache;

import com.github.benmanes.caffeine.cache.CacheLoader;
import manifold.api.host.AbstractTypeSystemListener;
import manifold.api.host.RefreshRequest;
import manifold.internal.host.ManifoldHost;
import manifold.util.concurrent.Cache;

public class TypeSystemAwareCache<K, V> extends Cache<K, V>
{
  @SuppressWarnings({"FieldCanBeLocal"})
  private final AbstractTypeSystemListener _cacheClearer = new CacheClearer( this );

  public static <K, V> TypeSystemAwareCache<K, V> make( String name, int size, CacheLoader<K, V> loader )
  {
    return new TypeSystemAwareCache<>( name, size, loader );
  }

  public TypeSystemAwareCache( String name, int size, CacheLoader<K, V> loader )
  {
    super( name, size, loader );
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
