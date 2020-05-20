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

package manifold.collections.extensions.java.util.stream.Stream;

import java.util.function.Supplier;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Extension
public class ManifoldStreamCollectionsExt
{
  public static <T> List<T> toList(@This Stream<T> thiz)
  {
    return thiz.collect(Collectors.toList());
  }

  /**
   * @return A set containing all the elements from the stream, retaining the order of the elements visited.
   */
  public static <T> Set<T> toSet(@This Stream<T> thiz)
  {
    return thiz.collect( (Supplier<Set<T>>)LinkedHashSet::new, Set::add, Set::addAll);
  }

  public static <T, K, V> Map<K, V> toMap(@This Stream<T> thiz, Function<? super T, K> keyMapper, Function<? super T, V> valueMapper)
  {
    return thiz.collect(Collectors.toMap(keyMapper, valueMapper));
  }

  public static <T, K> Map<K, T> toMap(@This Stream<T> thiz, Function<? super T, K> keyMapper)
  {
    return thiz.collect(Collectors.toMap(keyMapper, Function.identity()));
  }

  public static <T, V> Map<V, List<T>> groupingBy(@This Stream<T> thiz, Function<? super T, V> valueMapper)
  {
    return thiz.collect(Collectors.groupingBy(valueMapper));
  }
}
