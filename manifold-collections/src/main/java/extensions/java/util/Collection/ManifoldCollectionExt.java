package extensions.java.util.Collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
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
  public static <E, R> Stream<R> map( @This Collection<E> thiz, Function<? super E, R> mapper )
  {
    return thiz.stream().map( mapper );
  }

  public static <E> Stream<E> filter( @This Collection<E> thiz, Predicate<? super E> predicate )
  {
    return thiz.stream().filter( predicate );
  }

  public static <E, R, A> R collect( @This Collection<E> thiz, Collector<? super E, A, R> collector )
  {
    return thiz.stream().collect( collector );
  }

  public static <E> Stream<E> distinct( @This Collection<E> thiz )
  {
    return thiz.stream().distinct();
  }

  public static <E> Stream<E> sorted( @This Collection<E> thiz )
  {
    return thiz.stream().sorted();
  }

  public static <E> Stream<E> sorted( @This Collection<E> thiz, Comparator<? super E> comparator )
  {
    return thiz.stream().sorted( comparator );
  }

  public static <E> E reduce( @This Collection<E> thiz, E identity, BinaryOperator<E> accumulator )
  {
    return thiz.stream().reduce( identity, accumulator );
  }

  public static <E> boolean anyMatch( @This Collection<E> thiz, Predicate<? super E> comparator )
  {
    return thiz.stream().anyMatch( comparator );
  }

  public static <E> boolean allMatch( @This Collection<E> thiz, Predicate<? super E> comparator )
  {
    return thiz.stream().allMatch( comparator );
  }

  public static <E> boolean noneMatch( @This Collection<E> thiz, Predicate<? super E> comparator )
  {
    return thiz.stream().noneMatch( comparator );
  }

  //=================================================================
  // Remove Optional
  //=================================================================

  public static <E> E reduce( @This Collection<E> thiz, BinaryOperator<E> accumulator )
  {
    return thiz.stream().reduce( accumulator ).orElse( null );
  }

  public static <E> E min( @This Collection<E> thiz, Comparator<? super E> comparator )
  {
    return thiz.stream().min( comparator ).orElse( null );
  }

  public static <E> E max( @This Collection<E> thiz, Comparator<? super E> comparator )
  {
    return thiz.stream().max( comparator ).orElse( null );
  }

  //=================================================================
  // Embellishments
  //=================================================================

  /**
   * Adds all elements of the given Iterable to this Collection
   */
  public static <E> boolean addAll( @This Collection<E> thiz, Iterable<E> elements )
  {
    if( elements instanceof Collection )
    {
      return thiz.addAll( (Collection<? extends E>)elements );
    }
    else
    {
      boolean result = false;
      for( E item : elements )
      {
        if( thiz.add( item ) )
        {
          result = true;
        }
      }
      return result;
    }
  }

  public static <E> String join( @This Collection<E> thiz, CharSequence delimiter )
  {
    return thiz.stream().map( ManObjectUtil::toString ).collect( Collectors.joining( delimiter ) );
  }

  public static <E> List<E> toList( @This Collection<E> thiz )
  {
    if( thiz.isEmpty() )
    {
      return Collections.emptyList();
    }
    else if( thiz.size() == 1 )
    {
      return Collections.singletonList( thiz.first() );
    }
    return new ArrayList<>( thiz );
  }

  public static <E> Set<E> toSet( @This Collection<E> thiz )
  {
    if( thiz.isEmpty() )
    {
      return Collections.emptySet();
    }
    else if( thiz.size() == 1 )
    {
      return Collections.singleton( thiz.first() );
    }
    return new LinkedHashSet<>( thiz );
  }

  public static <E> SortedSet<E> toSortedSet( @This Collection<E> thiz )
  {
    return new TreeSet<>( thiz );
  }

  public static <E, K, V> Map<K, V> toMap( @This Collection<E> thiz, Function<? super E, K> keyMapper, Function<? super E, V> valueMapper )
  {
    return thiz.stream().collect( Collectors.toMap( keyMapper, valueMapper ) );
  }

  public static <E, K> Map<K, E> toMap( @This Collection<E> thiz, Function<? super E, K> keyMapper )
  {
    return thiz.stream().collect( Collectors.toMap( keyMapper, Function.identity() ) );
  }

  public static <E, V> Map<V, List<E>> groupingBy( @This Collection<E> thiz, Function<? super E, V> valueMapper )
  {
    return thiz.stream().collect( Collectors.groupingBy( valueMapper ) );
  }

}
