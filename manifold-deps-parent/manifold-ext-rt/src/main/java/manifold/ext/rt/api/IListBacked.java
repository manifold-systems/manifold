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

package manifold.ext.rt.api;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * A {@link Structural} interface with only list methods can extend this interface and provide default
 * implementations of its methods and implement a compile-time proxy API to avoid the overhead runtime proxy
 * generation.
 * <p/>
 * See the {@code JsonStructureType}.
 */
public interface IListBacked<T> extends List<T>
{
  /**
   * The {@link List} object used to store values corresponding with List methods.
   */
  List<T> getList();


  /*
   * Delegate to the wrapped List.
   */

  @Override
  default void replaceAll( UnaryOperator<T> operator )
  {
    getList().replaceAll( operator ); 
  }

  @Override
  default void sort( Comparator<? super T> c )
  {
    getList().sort( c );
  }

  @Override
  default Spliterator<T> spliterator()
  {
    return getList().spliterator();
  }

  @Override
  default boolean removeIf( Predicate<? super T> filter )
  {
    return removeIf( filter );
  }

  @Override
  default Stream<T> stream()
  {
    return getList().stream();
  }

  @Override
  default Stream<T> parallelStream()
  {
    return getList().parallelStream();
  }

  @Override
  default void forEach( Consumer<? super T> action )
  {
    getList().forEach( action );
  }

  @Override
  default int size()
  {
    return getList().size();
  }

  @Override
  default boolean isEmpty()
  {
    return getList().isEmpty();
  }

  @Override
  default boolean contains( Object o )
  {
    return getList().contains( o );
  }

  @Override
  default Iterator<T> iterator()
  {
    return getList().iterator();
  }

  @Override
  default Object[] toArray()
  {
    return getList().toArray();
  }

  @Override
  default <T1> T1[] toArray( T1[] a )
  {
    return getList().toArray( a );
  }

  @Override
  default boolean add( T t )
  {
    return getList().add( t );
  }

  @Override
  default boolean remove( Object o )
  {
    return getList().remove( o );
  }

  @Override
  default boolean containsAll( Collection<?> c )
  {
    return getList().containsAll( c );
  }

  @Override
  default boolean addAll( Collection<? extends T> c )
  {
    return getList().addAll( c );
  }

  @Override
  default boolean addAll( int index, Collection<? extends T> c )
  {
    return getList().addAll( index, c );
  }

  @Override
  default boolean removeAll( Collection<?> c )
  {
    return getList().removeAll( c );
  }

  @Override
  default boolean retainAll( Collection<?> c )
  {
    return getList().retainAll( c );
  }

  @Override
  default void clear()
  {
    getList().clear();
  }

  @Override
  default T get( int index )
  {
    return getList().get( index );
  }

  @Override
  default T set( int index, T element )
  {
    return getList().set( index, element );
  }

  @Override
  default void add( int index, T element )
  {
    getList().add( index, element );
  }

  @Override
  default T remove( int index )
  {
    return getList().remove( index );
  }

  @Override
  default int indexOf( Object o )
  {
    return getList().indexOf( o );
  }

  @Override
  default int lastIndexOf( Object o )
  {
    return getList().lastIndexOf( o );
  }

  @Override
  default ListIterator<T> listIterator()
  {
    return getList().listIterator();
  }

  @Override
  default ListIterator<T> listIterator( int index )
  {
    return getList().listIterator( index );
  }

  @Override
  default List<T> subList( int fromIndex, int toIndex )
  {
    return getList().subList( fromIndex, toIndex );
  }
}
