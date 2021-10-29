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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class FqnCacheNode<K>
{
  private final String _name;
  private final FqnCacheNode<K> _parent;
  private K _userData;
  private Map<String, FqnCacheNode<K>> _children;

  public FqnCacheNode( String text, FqnCacheNode<K> parent )
  {
    _name = text;
    _parent = parent;
  }

  public final FqnCacheNode<K> getChild( String segment )
  {
    if( _children != null )
    {
      return _children.get( segment );
    }
    else
    {
      return null;
    }
  }

  public FqnCacheNode<K> getParent()
  {
    return _parent;
  }

  public void clear()
  {
    _children = null;
    invalidate();
  }

  protected void invalidate()
  {
    if( _parent != null )
    {
      _parent.invalidate();
    }
  }

  public FqnCacheNode<K> getOrCreateChild( String child )
  {
    return getOrCreateChild( child, null, false );
  }
  public FqnCacheNode<K> getOrCreateChild( String child, K userData )
  {
    return getOrCreateChild( child, userData, true );
  }
  private FqnCacheNode<K> getOrCreateChild( String child, K userData, boolean setUserData )
  {
    if( _children == null )
    {
      _children = new ConcurrentHashMap<>( 2 );
    }
    FqnCacheNode<K> node = _children.get( child );
    if( node == null )
    {
      node = new FqnCacheNode<>( child, this );
      if( setUserData )
      {
        // adding a child and setting userData must be an atomic operation,
        // therefore userdata is assigned before child is added
        node.setUserData( userData );
      }
      _children.put( child, node );
      invalidate();
    }
    else if( setUserData )
    {
      node.setUserData( userData );
    }

    return node;
  }

  public final void delete()
  {
    _parent.deleteChild( this );
  }

  private void deleteChild( FqnCacheNode<K> child )
  {
    if( _children != null )
    {
      FqnCacheNode<K> removed = _children.remove( child._name );
      if( removed != null )
      {
        // update reverse cache
        removed.setUserData( null );
        invalidate();
      }
      if( _children.isEmpty() )
      {
        _children = null;
      }
    }
  }

  public final K getUserData()
  {
    return _userData;
  }

  public final void setUserData( K userData )
  {
    _userData = userData;
  }

  public final boolean isEmpty()
  {
    return isLeaf();
  }

  public final boolean isLeaf()
  {
    return _children == null || _children.isEmpty();
  }

  protected void collectNames( Set<String> names, String s )
  {
    if( _children != null )
    {
      for( FqnCacheNode<K> child : _children.values() )
      {
        String path = s.length() == 0
                      ? child._name
                      : s + child.separator() + child._name;
        if( child.isLeaf() )
        {
          names.add( path );
        }
        else
        {
          child.collectNames( names, path );
        }
      }
    }
  }

  public final Collection<FqnCacheNode<K>> getChildren()
  {
    if( _children != null )
    {
      return _children.values();
    }
    else
    {
      return Collections.emptySet();
    }
  }

  public final boolean visitDepthFirst( Predicate<K> visitor )
  {
    if( _children != null )
    {
      List<FqnCacheNode<K>> copy = new ArrayList<>( _children.values() );
      for( FqnCacheNode<K> child : copy )
      {
        if( !child.visitDepthFirst( visitor ) )
        {
          return false;
        }
      }
    }
    return visitor.test( getUserData() );
  }

  public final boolean visitNodeDepthFirst( Predicate<FqnCacheNode> visitor )
  {
    if( _children != null )
    {
      List<FqnCacheNode<K>> copy = new ArrayList<>( _children.values() );
      for( FqnCacheNode<K> child : copy )
      {
        if( !child.visitNodeDepthFirst( visitor ) )
        {
          return false;
        }
      }
    }
    return visitor.test( this );
  }

  public final String getName()
  {
    return _name;
  }

  public final String getFqn()
  {
    StringBuilder sb = new StringBuilder();
    FqnCacheNode<K> node = this;
    while( node != null && node.isVisible() )
    {
      String str = node._name + (sb.length() == 0 ? "" : separator());
      sb.insert( 0, str );
      node = node._parent;
    }
    return sb.toString();
  }

  private String separator()
  {
    char c = _name.charAt( 0 );
    return c == '[' || c == '<' ? "" : ".";
  }

  private boolean isVisible()
  {
    return getParent() != null || isRootVisible();
  }

  public boolean isRootVisible()
  {
    return false;
  }

  @Override
  public String toString()
  {
    return _name;
  }
}
