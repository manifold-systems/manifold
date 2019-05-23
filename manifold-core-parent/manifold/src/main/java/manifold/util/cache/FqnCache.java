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

import java.lang.ref.SoftReference;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import manifold.util.DynamicArray;
import manifold.util.concurrent.Cache;

public class FqnCache<T> extends FqnCacheNode<T> implements IFqnCache<T>
{
  private static final Cache<String, String[]> PARTS_CACHE =
    Cache.make( "Fqn Parts Cache", 10000, e -> FqnCache.split( e, null ) );

  private final Validator _validator;
  private final Cache<String, String[]> _validatorCache;
  private final boolean _rootVisible;
  private SoftReference<Set<String>> _allNames;

  public FqnCache()
  {
    this( "root", false, null );
  }

  public FqnCache( String name, boolean rootVisible, Validator validator )
  {
    super( name, null );
    _rootVisible = rootVisible;
    _validator = validator;
    _validatorCache = validator == null
                      ? null
                      : Cache.make( "FqnCache Parts", 10000, s -> FqnCache.split( s, _validator ) );
  }

  @Override
  public boolean isRootVisible()
  {
    return _rootVisible;
  }

  public FqnCacheNode<T> getNode( String fqn )
  {
    FqnCacheNode<T> n = this;
    for( String part : getParts( fqn, _validator ) )
    {
      n = n.getChild( part );
      if( n == null )
      {
        break;
      }
    }
    return n;
  }

  @Override
  public final T get( String fqn )
  {
    FqnCacheNode<T> n = getNode( fqn );
    return n == null ? null : n.getUserData();
  }

  @Override
  public final boolean contains( String fqn )
  {
    return getNode( fqn ) != null;
  }

  @Override
  public final void add( String fqn )
  {
    add( fqn, null );
  }

  @Override
  public void add( String fqn, T userData )
  {
    FqnCacheNode<T> n = this;
    String[] parts = getParts( fqn, _validator );
    for( int i = 0; i < parts.length; i++ )
    {
      String part = parts[i];
      if( i < parts.length - 1 )
      {
        n = n.getOrCreateChild( part );
      }
      else
      {
        n = n.getOrCreateChild( part, userData );
      }
    }
  }

  public void addAll( FqnCache<T> from )
  {
    from.getFqns().forEach( fqn -> add( fqn, from.get( fqn ) ) );
  }

  public void addAll( Map<String, T> from )
  {
    from.keySet().forEach( fqn -> add( fqn, from.get( fqn ) ) );
  }

  @Override
  public final void remove( String[] fqns )
  {
    for( String fqn : fqns )
    {
      remove( fqn );
    }
  }

  @Override
  public boolean remove( String fqn )
  {
    FqnCacheNode<T> n = this;
    for( String part : getParts( fqn, _validator ) )
    {
      n = n.getChild( part );
      if( n == null )
      {
        return false;
      }
    }
    n.delete();
    return true;
  }

  @Override
  protected void invalidate()
  {
    _allNames = null;
  }

  @Override
  public Set<String> getFqns()
  {
    Set<String> names = _allNames == null ? null : _allNames.get();
    if( names == null )
    {
      collectNames( names = new HashSet<>(), "" );
      _allNames = new SoftReference<>( names );
    }
    return names;
  }

  private static String[] split( String fqn, Validator validator )
  {
    String theRest = fqn;
    DynamicArray<String> parts = new DynamicArray<>();
    while( theRest != null )
    {
      int iParam = theRest.indexOf( '<' );
      int iDot = theRest.indexOf( '.' );
      int iArray = theRest.indexOf( '[' );
      String part;
      if( iParam == 0 )
      {
        if( iArray > 0 )
        {
          part = theRest.substring( 0, iArray );
          theRest = iArray < theRest.length() ? theRest.substring( iArray ) : null;
        }
        else
        {
          if( theRest.charAt( theRest.length() - 1 ) != '>' )
          {
            throw new IllegalTypeNameException( "\"" + theRest + "\" does not end with '>'" );
          }
          part = theRest;
          theRest = null;
        }
      }
      else if( iArray == 0 )
      {
        part = theRest.substring( 0, 2 );
        theRest = part.length() == theRest.length() ? null : theRest.substring( 2 );
      }
      else if( iParam > 0 )
      {
        if( iDot > 0 && iDot < iParam )
        {
          part = theRest.substring( 0, iDot );
          theRest = iDot + 1 < theRest.length() ? theRest.substring( iDot + 1 ) : null;
        }
        else
        {
          part = theRest.substring( 0, iParam );
          theRest = iParam < theRest.length() ? theRest.substring( iParam ) : null;
        }
      }
      else if( iDot > 0 )
      {
        part = theRest.substring( 0, iDot );
        theRest = iDot + 1 < theRest.length() ? theRest.substring( iDot + 1 ) : null;
      }
      else
      {
        part = theRest;
        theRest = null;
      }
      if( validator != null )
      {
        part = validator.validate( part );
        if( part == null )
        {
          return null;
        }
      }
      parts.add( StringCache.get( part ) );
    }

    return parts.toArray( new String[0] );
  }

  private String[] getParts( String fqn, Validator validator )
  {
    if( validator != null )
    {
      return _validatorCache.get( fqn );
    }
    return getParts( fqn );
  }

  public static String[] getParts( String fqn )
  {
    return PARTS_CACHE.get( fqn );
  }

  public interface Validator
  {
    String validate( String part );
  }
}
