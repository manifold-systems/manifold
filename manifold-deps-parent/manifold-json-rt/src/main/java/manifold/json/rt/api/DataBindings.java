/*
 * Copyright (c) 2020 - Manifold Systems LLC
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

package manifold.json.rt.api;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import manifold.rt.api.Bindings;

/**
 * A simple name/value bindings impl.
 */
public class DataBindings implements Bindings
{
  /**
   * Stores name/value bindings.
   */
  private Map<String, Object> _map;

  /**
   * Uses provided {@code Map} to store bindings.
   *
   * @param map The {@code Map} backing this {@link DataBindings}.
   *
   * @throws NullPointerException if map is null
   */
  public DataBindings( Map<String, Object> map )
  {
    if( map == null )
    {
      throw new NullPointerException();
    }
    _map = map;
  }

  /**
   * Default constructor uses a {@code HashMap}.
   */
  public DataBindings()
  {
    this( new LinkedHashMap<>() );
  }

  /**
   * Construct with initial size.
   *
   * @param size the initial size of the bindings
   */
  public DataBindings( int size )
  {
    this( new LinkedHashMap<>( size ) );
  }

  /**
   * Sets the specified key/value in the underlying {@code map} field.
   *
   * @param name  Name of value
   * @param value Value to set.
   *
   * @return Previous value for the specified key.  Returns null if key was previously
   * unset.
   *
   * @throws NullPointerException     if the name is null.
   * @throws IllegalArgumentException if the name is empty.
   */
  public Object put( String name, Object value )
  {
    checkKey( name );
    return _map.put( name, value );
  }

  /**
   * {@code putAll} is implemented using <code>Map.putAll</code>.
   *
   * @param toMerge The {@code Map} of values to add.
   *
   * @throws NullPointerException     if toMerge map is null or if some key in the map is null.
   * @throws IllegalArgumentException if some key in the map is an empty String.
   */
  public void putAll( Map<? extends String, ?> toMerge )
  {
    if( toMerge == null )
    {
      throw new NullPointerException( "toMerge map is null" );
    }
    for( Map.Entry<? extends String, ?> entry: toMerge.entrySet() )
    {
      String key = entry.getKey();
      checkKey( key );
      put( key, entry.getValue() );
    }
  }

  /**
   * {@inheritDoc}
   */
  public void clear()
  {
    _map.clear();
  }

  /**
   * Returns <tt>true</tt> if this map contains a mapping for the specified
   * key.  More formally, returns <tt>true</tt> if and only if
   * this map contains a mapping for a key <tt>k</tt> such that
   * <tt>(key==null ? k==null : key.equals(k))</tt>.  (There can be
   * at most one such mapping.)
   *
   * @param key key whose presence in this map is to be tested.
   *
   * @return <tt>true</tt> if this map contains a mapping for the specified
   * key.
   *
   * @throws NullPointerException     if key is null
   * @throws ClassCastException       if key is not String
   * @throws IllegalArgumentException if key is empty String
   */
  public boolean containsKey( Object key )
  {
    checkKey( key );
    return _map.containsKey( key );
  }

  /**
   * {@inheritDoc}
   */
  public boolean containsValue( Object value )
  {
    return _map.containsValue( value );
  }

  /**
   * {@inheritDoc}
   */
  public Set<Entry<String, Object>> entrySet()
  {
    return _map.entrySet();
  }

  /**
   * Returns the value to which this map maps the specified key.  Returns
   * <tt>null</tt> if the map contains no mapping for this key.  A return
   * value of <tt>null</tt> does not <i>necessarily</i> indicate that the
   * map contains no mapping for the key; it's also possible that the map
   * explicitly maps the key to <tt>null</tt>.  The <tt>containsKey</tt>
   * operation may be used to distinguish these two cases.
   *
   * <p>More formally, if this map contains a mapping from a key
   * <tt>k</tt> to a value <tt>v</tt> such that <tt>(key==null ? k==null :
   * key.equals(k))</tt>, then this method returns <tt>v</tt>; otherwise
   * it returns <tt>null</tt>.  (There can be at most one such mapping.)
   *
   * @param key key whose associated value is to be returned.
   *
   * @return the value to which this map maps the specified key, or
   * <tt>null</tt> if the map contains no mapping for this key.
   *
   * @throws NullPointerException     if key is null
   * @throws ClassCastException       if key is not String
   * @throws IllegalArgumentException if key is empty String
   */
  public Object get( Object key )
  {
    checkKey( key );
    return _map.get( key );
  }

  /**
   * {@inheritDoc}
   */
  public boolean isEmpty()
  {
    return _map.isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  public Set<String> keySet()
  {
    return _map.keySet();
  }

  /**
   * Removes the mapping for this key from this map if it is present
   * (optional operation).   More formally, if this map contains a mapping
   * from key <tt>k</tt> to value <tt>v</tt> such that
   * <code>(key==null ?  k==null : key.equals(k))</code>, that mapping
   * is removed.  (The map can contain at most one such mapping.)
   *
   * <p>Returns the value to which the map previously associated the key, or
   * <tt>null</tt> if the map contained no mapping for this key.  (A
   * <tt>null</tt> return can also indicate that the map previously
   * associated <tt>null</tt> with the specified key if the implementation
   * supports <tt>null</tt> values.)  The map will not contain a mapping for
   * the specified  key once the call returns.
   *
   * @param key key whose mapping is to be removed from the map.
   *
   * @return previous value associated with specified key, or <tt>null</tt>
   * if there was no mapping for key.
   *
   * @throws NullPointerException     if key is null
   * @throws ClassCastException       if key is not String
   * @throws IllegalArgumentException if key is empty String
   */
  public Object remove( Object key )
  {
    checkKey( key );
    return _map.remove( key );
  }

  /**
   * {@inheritDoc}
   */
  public int size()
  {
    return _map.size();
  }

  /**
   * {@inheritDoc}
   */
  public Collection<Object> values()
  {
    return _map.values();
  }

  private void checkKey( Object key )
  {
    if( key == null )
    {
      throw new NullPointerException( "key can not be null" );
    }
    if( !(key instanceof String) )
    {
      throw new ClassCastException( "key should be a String" );
    }
    if( key.equals( "" ) )
    {
      throw new IllegalArgumentException( "key can not be empty" );
    }
  }

  @Override
  public boolean equals( Object o )
  {
    if( this == o )
    {
      return true;
    }
    if( o == null || getClass() != o.getClass() )
    {
      return false;
    }
    DataBindings that = (DataBindings)o;
    return Objects.equals( _map, that._map );
  }

  @Override
  public int hashCode()
  {
    return Objects.hash( _map );
  }
}
