/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.api.util.cache;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 */
public class WeakFqnCache<T> implements IFqnCache<T>
{
  private FqnCache<Reference<T>> _cache;

  public WeakFqnCache()
  {
    _cache = new FqnCache<>();
  }

  @Override
  public void add( String fqn )
  {
    add( fqn, null );
  }

  @Override
  public void add( String fqn, T userData )
  {
    Reference<T> ref = new SoftReference<>( userData );
    _cache.add( fqn, ref );
  }

  @Override
  public boolean remove( String fqn )
  {
    return _remove( fqn );
  }

  private boolean _remove( String fqn )
  {
    return _cache.remove( fqn );
  }

  @Override
  public T get( String fqn )
  {
    Reference<T> ref = _cache.get( fqn );
    return ref == null ? null : ref.get();
  }

  @Override
  public FqnCacheNode<Reference<T>> getNode( String fqn )
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
    Predicate<Reference<T>> delegate = node -> {
      T userData = node == null ? null : node.get();
      return visitor.test( userData );
    };
    List<FqnCacheNode<Reference<T>>> copy = new ArrayList<>( _cache.getChildren() );
    for( FqnCacheNode<Reference<T>> child : copy )
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
    List<FqnCacheNode<Reference<T>>> copy = new ArrayList<>( _cache.getChildren() );
    for( FqnCacheNode<Reference<T>> child : copy )
    {
      if( !child.visitNodeDepthFirst( visitor ) )
      {
        return false;
      }
    }
    return true;
  }
}
