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

package manifold.collections.extensions.java.util.Map;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;
import manifold.rt.api.util.Pair;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Extension
public class ManMapExt
{
  /**
   * Implements the indexed assignment operator for Map to enable the syntax: {@code map[key] = value}
   * <p>
   * @see Map#put(Object, Object)
   * <p>
   * @param key key with which the specified value is to be associated
   * @param value value to be associated with the specified key
   * @return the previous value associated with <tt>key</tt>, or
   *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
   *         (A <tt>null</tt> return can also indicate that the map
   *         previously associated <tt>null</tt> with <tt>key</tt>,
   *         if the implementation supports <tt>null</tt> values.)
   * @throws UnsupportedOperationException if the <tt>put</tt> operation
   *         is not supported by this map
   * @throws ClassCastException if the class of the specified key or value
   *         prevents it from being stored in this map
   * @throws NullPointerException if the specified key or value is null
   *         and this map does not permit null keys or values
   * @throws IllegalArgumentException if some property of the specified key
   *         or value prevents it from being stored in this map
   */
  public static <K,V> V set( @This Map<K,V> thiz, K key, V value )
  {
    return thiz.put( key, value );
  }

  /**
   * For use with the {@code key and value} binding expression syntax using {@link Pair#and}.
   * <br>
   * <pre><code>
   * import static manifold.rt.api.util.Pair.and;
   * Map&lt;String, Integer&gt; scores =
   *   mapOf("Moe" and 100, "Larry" and 107, "Curly" and 111);
   * </code></pre>
   * Returns a new read-only map with the specified contents, given as a list of pairs where the first value is the key and the second is the value.
   * <p/>
   * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
   * <p/>
   * Entries of the map are iterated in the order they were specified.
   * <p>
   * @see Map#put(Object, Object)
   * <p>
   * @param entries key/value pairs, for use with the {@code key to value} binding expression syntax via {@link Pair#TO}.
   * @return a new read-only, ordered map with the specified contents.
   * @throws UnsupportedOperationException if the <tt>put</tt> operation
   *         is not supported by this map
   * @throws ClassCastException if the class of the specified key or value
   *         prevents it from being stored in this map
   * @throws NullPointerException if the specified key or value is null
   *         and this map does not permit null keys or values
   * @throws IllegalArgumentException if some property of the specified key
   *         or value prevents it from being stored in this map
   */
  @SafeVarargs
  @Extension
  public static <K,V> Map<K,V> mapOf( Pair<K,V>... entries )
  {
    LinkedHashMap<K,V> map = new LinkedHashMap<>( entries.length );
    for( Pair<K,V> pair : entries )
    {
      map.put( pair.getFirst(), pair.getSecond() );
    }
    return Collections.unmodifiableMap( map );
  }
}
