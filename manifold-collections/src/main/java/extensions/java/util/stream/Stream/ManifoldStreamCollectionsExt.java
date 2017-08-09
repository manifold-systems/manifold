package extensions.java.util.stream.Stream;

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

  public static <T> Set<T> toSet(@This Stream<T> thiz)
  {
    return thiz.collect(Collectors.toSet());
  }

  public static <T> SortedSet<T> toSortedSet(@This Stream<T> thiz) {
    TreeSet<T> es = new TreeSet<>();
    thiz.forEachOrdered(es::add);
    return es;
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
