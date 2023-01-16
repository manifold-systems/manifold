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

package manifold.ext.rt.extensions.java.lang.Object;

import manifold.ext.rt.api.*;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Extension
public class ManObjectExt
{
  private static final Map<Class,Object> classToEmptyArray = new ConcurrentHashMap<>();

  /**
   * Use this method to type-safely access private and other inaccessible members of the receiver of the call.
   * @see Jailbreak
   */
  public static @Jailbreak @Self Object jailbreak( @This Object thiz )
  {
    return thiz;
  }

  /**
   * Get a cached empty array value for this class. Use this method to avoid calling {@code new Foobar[0]} in your code.
   * <pre><code>
   * Foobar[] empty = Foobar.emptyArray();
   * </code></pre>
   */
  public static <E> E[] emptyArray( @ThisClass Class<E> callingClass )
  {
    return (E[])classToEmptyArray.computeIfAbsent( callingClass, key -> Array.newInstance( key, 0 ) );
  }
}
