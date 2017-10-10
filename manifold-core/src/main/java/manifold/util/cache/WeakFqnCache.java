package manifold.util.cache;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 */
public class WeakFqnCache<T> implements IFqnCache<T>
{
  private FqnCache<WeakReference<T>> _cache;
  private ReferenceQueue<T> _queue;

  public WeakFqnCache()
  {
    _cache = new FqnCache<WeakReference<T>>();
    _queue = new ReferenceQueue<T>();
  }

  @Override
  public void add( String fqn )
  {
    add( fqn, null );
  }

  @Override
  public void add( String fqn, T userData )
  {
//    removeReleasedEntries();
    KeyedReference<T> ref = new KeyedReference<T>( fqn, userData, _queue );
    _cache.add( fqn, ref );
  }

  @Override
  public boolean remove( String fqn )
  {
//    removeReleasedEntries();
    return _remove( fqn );
  }

  private boolean _remove( String fqn )
  {
    return _cache.remove( fqn );
  }

  @Override
  public T get( String fqn )
  {
    WeakReference<T> ref = _cache.get( fqn );
    return ref == null ? null : ref.get();
  }

  @Override
  public FqnCacheNode<WeakReference<T>> getNode( String fqn )
  {
    return _cache.getNode( fqn );
  }

  @Override
  public boolean contains( String fqn )
  {
    return _cache.contains( fqn );
  }

  @Override
  public void remove( String[] fqns )
  {
//    removeReleasedEntries();
    _cache.remove( fqns );
  }

  @Override
  public void clear()
  {
    _cache.clear();
  }

  @Override
  public Set<String> getFqns()
  {
    return _cache.getFqns();
  }

  @Override
  public boolean visitDepthFirst( final Predicate<T> visitor )
  {
    Predicate<WeakReference<T>> delegate = new Predicate<WeakReference<T>>()
    {
      @Override
      public boolean test( WeakReference<T> node )
      {
        T userData = node == null ? null : node.get();
        return visitor.test( userData );
      }
    };
    List<FqnCacheNode<WeakReference<T>>> copy = new ArrayList<FqnCacheNode<WeakReference<T>>>( _cache.getChildren() );
    for( FqnCacheNode<WeakReference<T>> child : copy )
    {
      if( !child.visitDepthFirst( delegate ) )
      {
        return false;
      }
    }
    return true;
  }

  public boolean visitNodeDepthFirst( final Predicate<FqnCacheNode> visitor )
  {
    List<FqnCacheNode<WeakReference<T>>> copy = new ArrayList<FqnCacheNode<WeakReference<T>>>( _cache.getChildren() );
    for( FqnCacheNode<WeakReference<T>> child : copy )
    {
      if( !child.visitNodeDepthFirst( visitor ) )
      {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean visitBreadthFirst( final Predicate<T> visitor )
  {
    Predicate<WeakReference<T>> delegate = new Predicate<WeakReference<T>>()
    {
      @Override
      public boolean test( WeakReference<T> node )
      {
        T userData = node == null ? null : node.get();
        return visitor.test( userData );
      }
    };
    List<FqnCacheNode<WeakReference<T>>> copy = new ArrayList<FqnCacheNode<WeakReference<T>>>( _cache.getChildren() );
    for( FqnCacheNode<WeakReference<T>> child : copy )
    {
      child.visitBreadthFirst( delegate );
    }
    return true;
  }

  private static class KeyedReference<T> extends WeakReference<T>
  {
    private String _fqn;

    public KeyedReference( String fqn, T referent, ReferenceQueue<? super T> queue )
    {
      super( referent, queue );
      _fqn = fqn;
    }

    public boolean equals( final Object o )
    {
      if( this == o )
      {
        return true;
      }
      if( o == null || getClass() != o.getClass() )
      {
        return false;
      }

      final KeyedReference that = (KeyedReference)o;

      return _fqn.equals( that._fqn ) && equal( get(), that.get() );
    }

    private <T> boolean equal( T p1, T p2 )
    {
      if( p1 == null || p2 == null )
      {
        return p1 == p2;
      }
      else if( p1 instanceof Object[] && p2 instanceof Object[] )
      {
        Object[] arr1 = (Object[])p1;
        Object[] arr2 = (Object[])p2;
        return Arrays.equals( arr1, arr2 );
      }
      else
      {
        return p1.equals( p2 );
      }
    }

    public int hashCode()
    {
      return _fqn.hashCode();
    }
  }

//  private void removeReleasedEntries() {
//    while( true ) {
//      KeyedReference<T> ref = (KeyedReference<T>)_queue.poll();
//      if( ref == null ) {
//        break;
//      }
//      FqnCacheNode<WeakReference<T>> node = getNode( ref._fqn );
//      if( node != null && node.isLeaf() && node.getUserData() == ref ) {
//        _remove( ref._fqn );
//      //  System.out.println( "XXXXXX: " + (++_removed) + " : " + ref._fqn );
//      }
//    }
//  }

}
