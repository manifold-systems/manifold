package manifold.collections.extensions.java.util.List;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import manifold.ext.api.Extension;
import manifold.ext.api.This;


import static java.util.Collections.emptyList;

/**
 */
@Extension
public class ManifoldListCollectionExt
{
  /**
   * Returns first element.
   *
   * @throws NoSuchElementException if the list is empty.
   */
  public static <E> E first( @This List<E> thiz )
  {
    if( thiz.isEmpty() )
    {
      throw new NoSuchElementException( "List is empty." );
    }
    return thiz.get( 0 );
  }

  /**
   * Returns the first element, or null if the list is empty.
   */
  public static <E> E firstOrNull( @This List<E> thiz )
  {
    return thiz.isEmpty() ? null : thiz.get( 0 );
  }

  /**
   * Returns the last element.
   *
   * @throws NoSuchElementException if the list is empty.
   */
  public static <E> E last( @This List<E> thiz )
  {
    if( thiz.isEmpty() )
    {
      throw new NoSuchElementException( "List is empty." );
    }
    return thiz.get( thiz.size() - 1 );
  }

  /**
   * Returns the last element matching the given [predicate].
   *
   * @throws [NoSuchElementException] if no such element is found.
   */
  public static <E> E last( @This List<E> thiz, Predicate<E> predicate )
  {
    ListIterator<E> iterator = thiz.listIterator( thiz.size() );
    while( iterator.hasPrevious() )
    {
      E element = iterator.previous();
      if( predicate.test( element ) )
      {
        return element;
      }
    }
    throw new NoSuchElementException( "List contains no element matching the predicate." );
  }

  /**
   * Returns the last element, or `null` if the list is empty.
   */
  public static <E> E lastOrNull( @This List<E> thiz )
  {
    return thiz.isEmpty() ? null : thiz.get( thiz.size() - 1 );
  }

  /**
   * Returns the last element matching the given [predicate], or `null` if no such element was found.
   */
  public static <E> E lastOrNull( @This List<E> thiz, Predicate<E> predicate )
  {
    ListIterator<E> iterator = thiz.listIterator( thiz.size() );
    while( iterator.hasPrevious() )
    {
      E element = iterator.previous();
      if( predicate.test( element ) )
      {
        return element;
      }
    }
    return null;
  }

  /**
   * Returns the single element, or throws an exception if the list is empty or has more than one element.
   */
  public static <E> E single( @This List<E> thiz )
  {
    switch( thiz.size() )
    {
      case 0:
        throw new NoSuchElementException( "List is empty." );
      case 1:
        return thiz.get( 0 );
      default:
        throw new IllegalArgumentException( "List has more than one element." );
    }
  }

  /**
   * Returns single element, or `null` if the list is empty or has more than one element.
   */
  public static <E> E singleOrNull( @This List<E> thiz )
  {
    return thiz.size() == 1 ? thiz.get( 0 ) : null;
  }

  /**
   * Returns an element at the given [index] or null if the [index] is out of bounds of this list.
   */
  public static <E> E getOrNull( @This List<E> thiz, int index )
  {
    return index >= 0 && index < thiz.size() ? thiz.get( index ) : null;
  }

  /**
   * Reverses elements in the list in-place.
   */
  public static <E> void reverse( @This List<E> thiz )
  {
    java.util.Collections.reverse( thiz );
  }

  public static <E> List<E> optimizeReadOnlyList( @This List<E> thiz )
  {
    switch( thiz.size() )
    {
      case 0:
        return emptyList();
      case 1:
        return Collections.singletonList( thiz.get( 0 ) );
      default:
        return Collections.unmodifiableList( thiz );
    }
  }
}
