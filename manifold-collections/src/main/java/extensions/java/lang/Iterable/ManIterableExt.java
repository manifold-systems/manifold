package extensions.java.lang.Iterable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import manifold.ext.api.Extension;
import manifold.ext.api.This;
import manifold.util.IndexedConsumer;
import manifold.util.IndexedFunction;
import manifold.util.IndexedPredicate;
import manifold.util.Pair;


import static java.util.Collections.emptyList;

/**
 */
@Extension
public class ManIterableExt
{
  /**
   * Returns first element.
   *
   * @throws NoSuchElementException if the collection is empty.
   */
  public static <T> T first( @This Iterable<T> thiz )
  {
    if( thiz instanceof List )
    {
      return ((List<T>)thiz).first();
    }
    else
    {
      Iterator<T> iterator = thiz.iterator();
      if( !iterator.hasNext() )
      {
        throw new NoSuchElementException( "Collection is empty." );
      }
      return iterator.next();
    }
  }

  /**
   * Returns the first element matching the given [predicate].
   *
   * @throws [NoSuchElementException] if no such element is found.
   */
  public static <T> T first( @This Iterable<T> thiz, Predicate<T> predicate )
  {
    for( T element : thiz )
    {
      if( predicate.test( element ) )
      {
        return element;
      }
    }
    throw new NoSuchElementException( "Collection contains no element matching the predicate." );
  }

  /**
   * Returns index of the first element matching the given [predicate], or -1 if the collection does not contain such element.
   */
  public static <T> int indexOfFirst( @This Iterable<T> thiz, Predicate<T> predicate )
  {
    int index = 0;
    for( T item : thiz )
    {
      if( predicate.test( item ) )
      {
        return index;
      }
      index++;
    }
    return -1;
  }

  /**
   * Returns the first element, or null if the collection is empty.
   */
  public static <T> T firstOrNull( @This Iterable<T> thiz )
  {
    if( thiz instanceof List )
    {
      if( ((List<T>)thiz).isEmpty() )
      {
        return null;
      }
      else
      {
        return ((List<T>)thiz).get( 0 );
      }
    }
    else
    {
      Iterator<T> iterator = thiz.iterator();
      if( !iterator.hasNext() )
      {
        return null;
      }
      return iterator.next();
    }
  }

  /**
   * Returns the first element matching the given [predicate], or `null` if element was not found.
   */
  public static <T> T firstOrNull( @This Iterable<T> thiz, Predicate<T> predicate )
  {
    for( T element : thiz )
    {
      if( predicate.test( element ) )
      {
        return element;
      }
    }
    return null;
  }

  /**
   * Returns the last element.
   *
   * @throws NoSuchElementException if the collection is empty.
   */
  public static <T> T last( @This Iterable<T> thiz )
  {
    if( thiz instanceof List )
    {
      return ((List<T>)thiz).last();
    }
    else
    {
      Iterator<T> iterator = thiz.iterator();
      if( !iterator.hasNext() )
      {
        throw new NoSuchElementException( "Collection is empty." );
      }
      T last = iterator.next();
      while( iterator.hasNext() )
      {
        last = iterator.next();
      }
      return last;
    }
  }

  /**
   * Returns the last element matching the given [predicate].
   *
   * @throws NoSuchElementException if no such element is found.
   */
  public static <T> T last( @This Iterable<T> thiz, Predicate<T> predicate )
  {
    T last = null;
    boolean found = false;
    for( T element : thiz )
    {
      if( predicate.test( element ) )
      {
        last = element;
        found = true;
      }
    }
    if( !found )
    {
      throw new NoSuchElementException( "Collection contains no element matching the predicate." );
    }
    return last;
  }

  /**
   * Returns index of the last element matching the given [predicate], or -1 if the collection does not contain such element.
   */
  public static <T> int indexOfLast( @This Iterable<T> thiz, Predicate<T> predicate )
  {
    int lastIndex = -1;
    int index = 0;
    for( T item : thiz )
    {
      if( predicate.test( item ) )
      {
        lastIndex = index;
      }
      index++;
    }
    return lastIndex;
  }

  /**
   * Returns the last element, or `null` if the collection is empty.
   */
  public static <T> T lastOrNull( @This Iterable<T> thiz )
  {
    if( thiz instanceof List )
    {
      return ((List<T>)thiz).isEmpty() ? null : ((List<T>)thiz).get( ((List)thiz).size() - 1 );
    }
    else
    {
      Iterator<T> iterator = thiz.iterator();
      if( !iterator.hasNext() )
      {
        return null;
      }
      T last = iterator.next();
      while( iterator.hasNext() )
      {
        last = iterator.next();
      }
      return last;
    }
  }

  /**
   * Returns the last element matching the given [predicate], or `null` if no such element was found.
   */
  public static <T> T lastOrNull( @This Iterable<T> thiz, Predicate<T> predicate )
  {
    T last = null;
    for( T element : thiz )
    {
      if( predicate.test( element ) )
      {
        last = element;
      }
    }
    return last;
  }

  /**
   * Returns the single element, or throws an exception if the collection is empty or has more than one element.
   */
  public static <T> T single( @This Iterable<T> thiz )
  {
    if( thiz instanceof List )
    {
      return ((List<T>)thiz).single();
    }
    else
    {
      Iterator<T> iterator = thiz.iterator();
      if( !iterator.hasNext() )
      {
        throw new NoSuchElementException( "Collection is empty." );
      }
      T single = iterator.next();
      if( iterator.hasNext() )
      {
        throw new IllegalArgumentException( "Collection has more than one element." );
      }
      return single;
    }
  }

  /**
   * Returns the single element matching the given [predicate], or throws exception if there is no or more than one matching element.
   */
  public static <T> T single( @This Iterable<T> thiz, Predicate<T> predicate )
  {
    T single = null;
    boolean found = false;
    for( T element : thiz )
    {
      if( predicate.test( element ) )
      {
        if( found )
        {
          throw new IllegalArgumentException( "Collection contains more than one matching element." );
        }
        single = element;
        found = true;
      }
    }
    if( !found )
    {
      throw new NoSuchElementException( "Collection contains no element matching the predicate." );
    }
    return single;
  }

  /**
   * Returns single element, or `null` if the collection is empty or has more than one element.
   */
  public static <T> T singleOrNull( @This Iterable<T> thiz )
  {
    if( thiz instanceof List )
    {
      return ((List<T>)thiz).size() == 1 ? ((List<T>)thiz).get( 0 ) : null;
    }
    else
    {
      Iterator<T> iterator = thiz.iterator();
      if( !iterator.hasNext() )
      {
        return null;
      }
      T single = iterator.next();
      if( iterator.hasNext() )
      {
        return null;
      }
      return single;
    }
  }

  /**
   * Returns the single element matching the given [predicate], or `null` if element was not found or more than one element was found.
   */
  public static <T> T singleOrNull( @This Iterable<T> thiz, Predicate<T> predicate )
  {
    T single = null;
    boolean found = false;
    for( T element : thiz )
    {
      if( predicate.test( element ) )
      {
        if( found )
        {
          return null;
        }
        single = element;
        found = true;
      }
    }
    if( !found )
    {
      return null;
    }
    return single;
  }

  /**
   * Returns a list containing all elements matching the given [predicate].
   */
  public static <T> List<T> filterToList( @This Iterable<T> thiz, Predicate<T> predicate )
  {
    return thiz.filterTo( new ArrayList<>(), predicate );
  }

  /**
   * Appends all elements matching the given [predicate] to the given [destination].
   */
  public static <T, C extends Collection<? super T>> C filterTo( @This Iterable<T> thiz, C destination, Predicate<T> predicate )
  {
    for( T element : thiz )
    {
      if( predicate.test( element ) )
      {
        destination.add( element );
      }
    }
    return destination;
  }

  /**
   * Returns a list containing only elements matching the given [predicate].
   *
   * @param predicate function that takes the index of an element and the element itself
   *                  and returns the result of predicate evaluation on the element.
   */
  public static <T> List<T> filterIndexedToList( @This Iterable<T> thiz, IndexedPredicate<T> predicate )
  {
    return thiz.filterIndexedTo( new ArrayList<T>(), predicate );
  }

  /**
   * Appends all elements matching the given [predicate] to the given [destination].
   *
   * @param predicate function that takes the index of an element and the element itself
   *                  and returns the result of predicate evaluation on the element.
   */
  public static <T, C extends Collection<? super T>> C filterIndexedTo( @This Iterable<T> thiz, C destination, IndexedPredicate<T> predicate )
  {
    thiz.forEachIndexed( ( index, element ) ->
                         {
                           if( predicate.test( index, element ) )
                           {
                             destination.add( element );
                           }
                         } );
    return destination;
  }

  /**
   * Returns a list containing all elements not matching the given [predicate].
   */
  public static <T> List<T> filterNotToList( @This Iterable<T> thiz, Predicate<T> predicate )
  {
    return thiz.filterNotTo( new ArrayList<T>(), predicate );
  }

  /**
   * Appends all elements not matching the given [predicate] to the given [destination].
   */
  public static <T, C extends Collection<? super T>> C filterNotTo( @This Iterable<T> thiz, C destination, Predicate<T> predicate )
  {
    for( T element : thiz )
    {
      if( !predicate.test( element ) )
      {
        destination.add( element );
      }
    }
    return destination;
  }

  /**
   * Returns a list with elements in reversed order.
   */
  public static <T> List<T> reversed( @This Iterable<T> thiz )
  {
    List<T> list = thiz.toList();
    if( list.size() <= 1 )
    {
      return list;
    }
    list.reverse();
    return list;
  }

  /**
   * Returns a [List] containing all elements.
   */
  public static <T> List<T> toList( @This Iterable<T> thiz )
  {
    if( thiz instanceof Collection )
    {
      return ((Collection<T>)thiz).toList();
    }
    ArrayList<T> list = new ArrayList<>();
    for( T elem : thiz )
    {
      list.add( elem );
    }
    return list;
  }

  /**
   * Returns a [Set] containing all unique elements.
   * <p>
   * The returned set preserves the element iteration order of the original collection.
   */
  public static <T> Set<T> toSet( @This Iterable<T> thiz )
  {
    if( thiz instanceof Collection )
    {
      return ((Collection<T>)thiz).toSet();
    }
    LinkedHashSet<T> set = new LinkedHashSet<>();
    for( T elem : thiz )
    {
      set.add( elem );
    }
    return set;
  }

  /**
   * Returns a single list of all elements yielded from results of [transform] function being invoked on each element of original collection.
   */
  public static <T, R> List<R> flatMap( @This Iterable<T> thiz, Function<T, Iterable<R>> transform )
  {
    return thiz.flatMapTo( new ArrayList<R>(), transform );
  }

  /**
   * Appends all elements yielded from results of [transform] function being invoked on each element of original collection, to the given [destination].
   */
  public static <T, R, C extends Collection<R>> C flatMapTo( @This Iterable<T> thiz, C destination, Function<T, Iterable<R>> transform )
  {
    for( T element : thiz )
    {
      Iterable<R> list = transform.apply( element );
      destination.addAll( list );
    }
    return destination;
  }

  /**
   * Returns a list containing only distinct elements from the given collection.
   * <p>
   * The elements in the resulting list are in the same order as they were in the source collection.
   */
  public static <T> List<T> distinctList( @This Iterable<T> thiz )
  {
    return thiz.toSet().toList();
  }

  /**
   * Returns a list containing only elements from the given collection
   * having distinct keys returned by the given [selector] function.
   * <p>
   * The elements in the resulting list are in the same order as they were in the source collection.
   */
  public static <T, K> List<T> distinctBy( @This Iterable<T> thiz, Function<T, K> selector )
  {
    HashSet<K> set = new HashSet<>();
    ArrayList<T> list = new ArrayList<>();
    for( T e : thiz )
    {
      K key = selector.apply( e );
      if( set.add( key ) )
      {
        list.add( e );
      }
    }
    return list;
  }

  /**
   * Returns a set containing all elements that are contained by both thiz set and the specified collection.
   * <p>
   * The returned set preserves the element iteration order of the original collection.
   */
  public static <T> Set<T> intersect( @This Iterable<T> thiz, Iterable<T> other )
  {
    Set<T> set = thiz.toSet();
    set.retainAll( coerceToUniqueCollection( other ) );
    return set;
  }

  /**
   * Returns a set containing all elements that are contained by thiz collection and not contained by the specified collection.
   * <p>
   * The returned set preserves the element iteration order of the original collection.
   */
  public static <T> Set<T> subtract( @This Iterable<T> thiz, Iterable<T> other )
  {
    Set<T> set = thiz.toSet();
    set.removeAll( coerceToUniqueCollection( other ) );
    return set;
  }

  /**
   * Returns a set containing all distinct elements from both collections.
   * <p>
   * The returned set preserves the element iteration order of the original collection.
   * Those elements of the [other] collection that are unique are iterated in the end
   * in the order of the [other] collection.
   */
  public static <T> Set<T> union( @This Iterable<T> thiz, Iterable<T> other )
  {
    Set<T> set = thiz.toSet();
    set.addAll( other );
    return set;
  }

  private static <T> Collection<T> coerceToUniqueCollection( Iterable<T> source )
  {
    if( source instanceof Collection && ((Collection)source).size() <= 1 )
    {
      return (Collection<T>)source;
    }
    HashSet<T> set = new HashSet<>();
    for( T elem : source )
    {
      set.add( elem );
    }
    return set;
  }

  /**
   * Returns the number of elements in thiz collection.
   */
  public static <T> int count( @This Iterable<T> thiz )
  {
    if( thiz instanceof Collection )
    {
      return ((Collection<T>)thiz).size();
    }

    int count = 0;
    for( T element : thiz )
    {
      count++;
    }
    return count;
  }

  /**
   * Returns the number of elements matching the given [predicate].
   */
  public static <T> int count( @This Iterable<T> thiz, Predicate<T> predicate )
  {
    int count = 0;
    for( T element : thiz )
    {
      if( predicate.test( element ) )
      {
        count++;
      }
    }
    return count;
  }

  /**
   * Returns the first element having the largest value according to the provided [comparator] or `null` if there are no elements.
   */
  public static <T> T maxWith( @This Iterable<T> thiz, Comparator<T> comparator )
  {
    Iterator<T> iterator = thiz.iterator();
    if( !iterator.hasNext() )
    {
      return null;
    }
    T max = iterator.next();
    while( iterator.hasNext() )
    {
      T e = iterator.next();
      if( comparator.compare( max, e ) < 0 )
      {
        max = e;
      }
    }
    return max;
  }

  /**
   * Returns the first element having the smallest value according to the provided [comparator] or `null` if there are no elements.
   */
  public static <T> T minWith( @This Iterable<T> thiz, Comparator<T> comparator )
  {
    Iterator<T> iterator = thiz.iterator();
    if( !iterator.hasNext() )
    {
      return null;
    }
    T min = iterator.next();
    while( iterator.hasNext() )
    {
      T e = iterator.next();
      if( comparator.compare( min, e ) > 0 )
      {
        min = e;
      }
    }
    return min;
  }

  /**
   * Splits the original collection into pair of lists,
   * where *first* list contains elements for which [predicate] yielded `true`,
   * while *second* list contains elements for which [predicate] yielded `false`.
   */
  public static <T> Pair<List<T>, List<T>> partition( @This Iterable<T> thiz, Predicate<T> predicate )
  {
    ArrayList<T> first = new ArrayList<T>();
    ArrayList<T> second = new ArrayList<T>();
    for( T element : thiz )
    {
      if( predicate.test( element ) )
      {
        first.add( element );
      }
      else
      {
        second.add( element );
      }
    }
    return new Pair<>( first, second );
  }

  private static <T> int collectionSizeOrDefault( Iterable<T> thiz, int def )
  {
    return thiz instanceof Collection ? ((Collection<T>)thiz).size() : def;
  }

  /**
   * Returns a list containing the results of applying the given [transform] function
   * to each element in the original collection.
   */
  public static <T, R> List<R> mapToList( @This Iterable<T> thiz, Function<T, R> transform )
  {
    return thiz.mapTo( new ArrayList<R>( collectionSizeOrDefault( thiz, 10 ) ), transform );
  }

  /**
   * Returns a list containing the results of applying the given [transform] function
   * to each element and its index in the original collection.
   *
   * @param transform function that takes the index of an element and the element itself
   *                  and returns the result of the transform applied to the element.
   */
  public static <T, R> List<R> mapIndexed( @This Iterable<T> thiz, IndexedFunction<T, R> transform )
  {
    return thiz.mapIndexedTo( new ArrayList<R>( collectionSizeOrDefault( thiz, 10 ) ), transform );
  }

  /**
   * Returns a list containing only the non-null results of applying the given [transform] function
   * to each element and its index in the original collection.
   *
   * @param transform function that takes the index of an element and the element itself
   *                  and returns the result of the transform applied to the element.
   */
  public static <T, R> List<R> mapIndexedNotNull( @This Iterable<T> thiz, IndexedFunction<T, R> transform )
  {
    return thiz.mapIndexedNotNullTo( new ArrayList<R>(), transform );
  }

  /**
   * Applies the given [transform] function to each element and its index in the original collection
   * and appends only the non-null results to the given [destination].
   *
   * @param transform function that takes the index of an element and the element itself
   *                  and returns the result of the transform applied to the element.
   */
  public static <T, R, C extends Collection<? super R>> C mapIndexedNotNullTo( @This Iterable<T> thiz, C destination, IndexedFunction<T, R> transform )
  {
    thiz.forEachIndexed( ( index, element ) ->
                         {
                           R result = transform.apply( index, element );
                           if( result != null )
                           {
                             destination.add( result );
                           }
                         } );
    return destination;
  }

  /**
   * Applies the given [transform] function to each element and its index in the original collection
   * and appends the results to the given [destination].
   *
   * @param transform function that takes the index of an element and the element itself
   *                  and returns the result of the transform applied to the element.
   */
  public static <T, R, C extends Collection<? super R>> C mapIndexedTo( @This Iterable<T> thiz, C destination, IndexedFunction<T, R> transform )
  {
    int index = 0;
    for( T item : thiz )
    {
      destination.add( transform.apply( index++, item ) );
    }
    return destination;
  }

  /**
   * Returns a list containing only the non-null results of applying the given [transform] function
   * to each element in the original collection.
   */
  public static <T, R> List<R> mapNotNull( @This Iterable<T> thiz, Function<T, R> transform )
  {
    return thiz.mapNotNullTo( new ArrayList<>(), transform );
  }

  /**
   * Applies the given [transform] function to each element in the original collection
   * and appends only the non-null results to the given [destination].
   */
  public static <T, R, C extends Collection<? super R>> C mapNotNullTo( @This Iterable<T> thiz, C destination, Function<T, R> transform )
  {
    thiz.forEach( element ->
                  {
                    R result = transform.apply( element );
                    if( result != null )
                    {
                      destination.add( result );
                    }
                  } );
    return destination;
  }

  /**
   * Applies the given [transform] function to each element of the original collection
   * and appends the results to the given [destination].
   */
  public static <T, R, C extends Collection<? super R>> C mapTo( @This Iterable<T> thiz, C destination, Function<T, R> transform )
  {
    for( T item : thiz )
    {
      destination.add( transform.apply( item ) );
    }
    return destination;
  }

  /**
   * Returns a list containing all the elmeents from [fromIndex] (inclusive)
   */
  public static <T> List<T> subList( @This Iterable<T> thiz, int fromIndex )
  {
    return thiz.subList( fromIndex, -1 );
  }

  /**
   * Returns a list containing the elmeents [fromIndex] (inclusive) to [toIndex] (exclusive)
   */
  public static <T> List<T> subList( @This Iterable<T> thiz, int fromIndex, int toIndex )
  {
    if( thiz instanceof Collection && ((Collection<T>)thiz).isEmpty() )
    {
      return emptyList();
    }
    boolean toEnd = toIndex < 0;
    if( thiz instanceof List )
    {
      //noinspection unchecked
      return ((List<T>)thiz).subList( fromIndex, !toEnd ? toIndex : ((List<T>)thiz).size() );
    }
    ArrayList<T> list = new ArrayList<>();
    Iterator<T> iter = thiz.iterator();
    for( int i = 0; (toEnd || i < toIndex) && iter.hasNext(); i++ )
    {
      T elem = iter.next();
      if( i >= fromIndex )
      {
        list.add( elem );
      }
    }
    return list.optimizeReadOnlyList();
  }

  /**
   * Performs the given [action] on each element, providing sequential index with the element.
   *
   * @param action function that takes the index of an element and the element itself
   *               and performs the desired action on the element.
   */
  public static <T> void forEachIndexed( @This Iterable<T> thiz, IndexedConsumer<T> action )
  {
    int index = 0;
    for( T item : thiz )
    {
      action.accept( index++, item );
    }
  }

  /**
   * Join the elements together in a String separated by [separator].
   */
  public static <T> String joinToString( @This Iterable<T> thiz, CharSequence separator )
  {
    return thiz.joinTo( new StringBuilder(), separator ).toString();
  }

  /**
   * Append the elements to [buffer] separated by [separator].
   */
  public static <T, A extends Appendable> A joinTo( @This Iterable<T> thiz, A buffer, CharSequence separator )
  {
    int count = 0;
    try
    {
      for( T e : thiz )
      {
        if( count++ > 0 )
        {
          buffer.append( separator );
        }
        buffer.append( e.toString() );
      }
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
    return buffer;
  }

  /**
   * Accumulates value starting with [initial] value and applying [operation] from left to right to current accumulator value and each element.
   * <p>
   * The operation is _terminal_.
   */
  public static <T, R> R fold( @This Iterable<T> thiz, R initial, BiFunction<R, T, R> operation )
  {
    R accumulator = initial;
    for( T element : thiz )
    {
      accumulator = operation.apply( accumulator, element );
    }
    return accumulator;
  }

}
