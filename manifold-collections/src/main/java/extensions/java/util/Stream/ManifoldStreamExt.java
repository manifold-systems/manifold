package extensions.java.util.Stream;

import manifold.ext.api.Extension;
import manifold.ext.api.This;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Extension
public class ManifoldStreamExt
{
  public static <E> List<E> toList(@This Stream<E> thiz)
  {
    return thiz.collect(Collectors.toList());
  }

  public static <E> Set<E> toSet(@This Stream<E> thiz)
  {
    return thiz.collect(Collectors.toSet());
  }

  public static <E> SortedSet<E> toSortedSet(@This Stream<E> thiz) {
    TreeSet<E> es = new TreeSet<>();
    thiz.forEachOrdered(es::add);
    return es;
  }

  public static <E, K, V> Map<K, V> toMap(@This Stream<E> thiz, Function<? super Object, K> keyMapper, Function<? super Object, V> valueMapper)
  {
    return thiz.collect(Collectors.toMap(keyMapper, valueMapper));
  }
}
