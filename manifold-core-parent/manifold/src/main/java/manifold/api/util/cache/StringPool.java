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

import java.util.concurrent.ConcurrentHashMap;

/**
 * Faster than String.intern()
 */
public class StringPool
{
  private static final StringPool INSTANCE = new StringPool();

  private ConcurrentHashMap<String, String> _map;
  private int _misses;
  private int _total;
  private long _size;

  private StringPool()
  {
    _map = new ConcurrentHashMap<>();
  }

  public static String get( String value )
  {
    String existing = INSTANCE._map.get( value );
    if( existing != null )
    {
      return existing;
    }
    INSTANCE._map.put( value, value );
    return value;
  }

//  public static String get( String value ) {
//    String existing = INSTANCE._map.get( value );
//    INSTANCE._total++;
//    if( existing != null ) {
//      return existing;
//    }
//    INSTANCE._misses++;
//    INSTANCE._size += value.length();
//    INSTANCE._map.put( value, value );
//    return value;
//  }

  public static void printStats()
  {
    System.out.println( "MISSES: " + INSTANCE._misses );
    System.out.println( "TOTAL: " + INSTANCE._total );
    System.out.println( "SIZE: " + INSTANCE._size );
  }
}
