package manifold.util.concurrent;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import manifold.util.ILogger;

/**
 * static var MY_CACHE = new Cache<Foo, Bar>( 1000, \ foo -> getBar( foo ) )
 */
public class Cache<K, V>
{
  private final String _name;
  private final int _size;
  private final CacheLoader<K, V> _loader;
  private LoadingCache<K, V> _cacheImpl;

  private ScheduledFuture<?> _loggingTask;

  public Cache( String name, int size, CacheLoader<K, V> loader )
  {
    _name = name;
    _size = size;
    _loader = loader;
    clearCacheImpl();
  }

  private void clearCacheImpl()
  {
    _cacheImpl = Caffeine.newBuilder().maximumSize( _size ).build( _loader );
  }

  /**
   * This will evict a specific key from the cache.
   *
   * @param key the key to evict
   */
  public void evict( K key )
  {
    _cacheImpl.invalidate( key );
  }

  /**
   * This will put a specific entry in the cache
   *
   * @param key   this is the key
   * @param value this is the value
   *
   * @return the old value for this key
   */
  public void put( K key, V value )
  {
    _cacheImpl.put( key, value );
  }

  /**
   * Gets a specific entry, calling the loader if no entry exists
   *
   * @param key the object to find
   *
   * @return the found object (may be null)
   */
  public V get( K key )
  {
    return _cacheImpl.get( key );
  }

  public CacheStats getStats()
  {
    return _cacheImpl.stats();
  }

  public String getName()
  {
    return _name;
  }

  /**
   * Sets up a recurring task every n seconds to report on the status of this cache.  This can be useful
   * if you are doing exploratory caching and wish to monitor the performance of this cache with minimal fuss.
   *
   * @param seconds how often to log the entry
   * @param logger  the logger to use
   *
   * @return this
   */
  public synchronized Cache<K, V> logEveryNSeconds( int seconds, final ILogger logger )
  {
    if( _loggingTask == null )
    {
      ScheduledExecutorService service = Executors.newScheduledThreadPool( 1 );
      _loggingTask = service.scheduleAtFixedRate( () -> logger.info( Cache.this ), seconds, seconds, TimeUnit.SECONDS );
    }
    else
    {
      throw new IllegalStateException( "Logging for " + this + " is already enabled" );
    }
    return this;
  }

  public synchronized void stopLogging()
  {
    if( _loggingTask != null )
    {
      _loggingTask.cancel( false );
    }
  }

  public void clear()
  {
    clearCacheImpl();
  }

  @Override
  public String toString()
  {
    return getStats().toString();
  }

  public static <KK, VV> Cache<KK, VV> make( String name, int size, CacheLoader<KK, VV> loader )
  {
    return new Cache<>( name, size, loader );
  }
}
