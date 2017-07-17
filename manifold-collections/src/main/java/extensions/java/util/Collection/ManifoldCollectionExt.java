package extensions.java.util.Collection;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import manifold.ext.api.Extension;
import manifold.ext.api.This;
import manifold.util.ManObjectUtil;

@Extension
public class ManifoldCollectionExt
{
  //=================================================================
  // Straight stream pass throughs
  //=================================================================
  public static <T, R> Stream<R> map(@This Collection<T> thiz, Function<? super T, R> mapper)
  {
    return thiz.stream().map(mapper);
  }

  public static <T> Stream<T> filter(@This Collection<T> thiz, Predicate<? super T> predicate)
  {
    return thiz.stream().filter(predicate);
  }

  public static <T, R, A> R collect(@This Collection<T> thiz, Collector<? super T, A, R> collector){
    return thiz.stream().collect(collector);
  }

  public static <T> Stream<T> distinct(@This Collection<T> thiz)
  {
    return thiz.stream().distinct();
  }

  public static <T> Stream<T> sorted(@This Collection<T> thiz)
  {
    return thiz.stream().sorted();
  }

  public static <T> Stream<T> sorted(@This Collection<T> thiz, Comparator<? super T> comparator)
  {
    return thiz.stream().sorted(comparator);
  }

  public static <T> T reduce(@This Collection<T> thiz, T identity, BinaryOperator<T> accumulator)
  {
    return thiz.stream().reduce(identity, accumulator);
  }

  public static <T> boolean anyMatch(@This Collection<T> thiz, Predicate<? super T> comparator)
  {
    return thiz.stream().anyMatch(comparator);
  }

  public static <T> boolean allMatch(@This Collection<T> thiz, Predicate<? super T> comparator)
  {
    return thiz.stream().allMatch(comparator);
  }

  public static <T> boolean noneMatch(@This Collection<T> thiz, Predicate<? super T> comparator)
  {
    return thiz.stream().noneMatch(comparator);
  }

  //=================================================================
  // Remove Optional
  //=================================================================

  public static <T> T reduce(@This Collection<T> thiz, BinaryOperator<T> accumulator)
  {
    return thiz.stream().reduce(accumulator).orElse(null);
  }

  public static <T> T min(@This Collection<T> thiz, Comparator<? super T> comparator)
  {
    return thiz.stream().min(comparator).orElse(null);
  }

  public static <T> T max(@This Collection<T> thiz, Comparator<? super T> comparator)
  {
    return thiz.stream().max(comparator).orElse(null);
  }

  //=================================================================
  // Embellishments
  //=================================================================

  public static <T> String join(@This Collection<T> thiz, CharSequence delimiter)
  {
    return thiz.stream().map(ManObjectUtil::toString).collect(Collectors.joining(delimiter));
  }

  public static <T> List<T> toList(@This Collection<T> thiz)
  {
    return new ArrayList<>(thiz);
  }

  public static <T> Set<T> toSet(@This Collection<T> thiz)
  {
    return new HashSet<>(thiz);
  }

  public static <T, K, V> Map<K, V> toMap(@This Collection<T> thiz, Function<? super Object, K> keyMapper, Function<? super Object, V> valueMapper)
  {
    return thiz.stream().collect(Collectors.toMap(keyMapper, valueMapper));
  }

}
