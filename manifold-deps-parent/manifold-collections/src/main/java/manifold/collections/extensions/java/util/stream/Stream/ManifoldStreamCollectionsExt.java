package manifold.collections.extensions.java.util.stream.Stream;

import java.util.function.Supplier;
import java.util.stream.Collector;
import manifold.ext.api.Extension;
import manifold.ext.api.This;

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
