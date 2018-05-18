package manifold.util.concurrent;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import manifold.util.ILogger;

/**
 * static var MY_CACHE = new Cache<Foo, Bar>( 1000, \ foo -> getBar( foo ) )
 */
public class Cache<K, V>
{
  private ConcurrentSkipListMap<K, V> _cacheImlp;
  private final MissHandler<K, V> _missHandler;
  private final String _name;
  private final int _size;

  //statistics
  private final AtomicInteger _requests = new AtomicInteger();
  private final AtomicInteger _misses = new AtomicInteger();
  private final AtomicInteger _hits = new AtomicInteger();

  private ScheduledFuture<?> _loggingTask;

  /**
   * This will create a new cache
   *
   * @param name        the name of the cache for logging
   * @param size        the maximum size of the log
   * @param missHandler how to handle misses, this is required not to be null
   */
  public Cache( String name, int size, MissHandler<K, V> missHandler )
  {
    _name = name;
    _size = size;
    clearCacheImpl();
    _missHandler = missHandler;
  }

  private void clearCacheImpl()
  {
    _cacheImlp = new ConcurrentSkipListMap<>();
  }

  /**
   * This will evict a specific key from the cache.
   *
   * @param key the key to evict
   *
   * @return the current value for that key
   */
  public V evict( K key )
  {
    return _cacheImlp.remove( key );
  }

  /**
   * This will put a specific entry in the cache
   *
   * @param key   this is the key
   * @param value this is the value
   *
   * @return the old value for this key
   */
  public V put( K key, V value )
  {
    return _cacheImlp.put( key, value );
  }

  /**
   * This will get a specific entry, it will call the missHandler if it is not found.
   *
   * @param key the object to find
   *
   * @return the found object (may be null)
   */
  public V get( K key )
  {
    V value = _cacheImlp.get( key );
    _requests.incrementAndGet();
    if( value == null )
    {
      value = _missHandler.load( key );
      _cacheImlp.put( key, value );
      _misses.incrementAndGet();
    }
    else
    {
      _hits.incrementAndGet();
    }
    return value;
  }

  public int getConfiguredSize()
  {
    return _size;
  }

  public int getUtilizedSize()
  {
    return _cacheImlp.size();
  }

  public int getRequests()
  {
    return _requests.get();
  }

  public int getMisses()
  {
    return _misses.get();
  }

  public int getHits()
  {
    return _hits.get();
  }

  public double getHitRate()
  {
    int requests = getRequests();
    int hits = getHits();
    if( requests == 0 )
    {
      return 0.0;
    }
    else
    {
      return ((double)hits) / requests;
    }
  }

  /**
   * Sets up a recurring task every n seconds to report on the status of this cache.  This can be useful
   * if you are doing exploratory caching and wish to monitor the performance of this cache with minimal fuss.
   * Consider
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
      _loggingTask = service.scheduleAtFixedRate( new Runnable()
      {
        public void run()
        {
          logger.info( Cache.this );
        }
      }, seconds, seconds, TimeUnit.SECONDS );
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

  public interface MissHandler<L, W>
  {
    W load( L key );
  }

  public void clear()
  {
    clearCacheImpl();
    _hits.set( 0 );
    _misses.set( 0 );
    _requests.set( 0 );
  }

  @Override
  public String toString()
  {
    return "Cache \"" + _name + "\"( Hits:" + getHits() + ", Misses:" + getMisses() + ", Requests:" + getRequests() + ", Hit rate:" + BigDecimal.valueOf( getHitRate() * 100.0 ).setScale( 2, BigDecimal.ROUND_DOWN ) + "% )";
  }

  public static <K, V> Cache<K, V> make( String name, int size, MissHandler<K, V> handler )
  {
    return new Cache<K, V>( name, size, handler );
  }
}
