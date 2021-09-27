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

import manifold.ext.rt.RuntimeMethods;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link Structural} interface with only list methods can extend this interface and provide default
 * implementations of its methods and implement a compile-time proxy API to avoid the overhead runtime proxy
 * generation.
 * <p/>
 * See the {@code JsonListType}.
 */
public interface IListBacked<T> extends List<T>
{
  /**
   * The {@link List} object used to store raw values corresponding with List methods.
   */
  List<Object> getList();

  Class<?> getFinalComponentType();

  /*
   * Delegate to the wrapped List.
   */

  default void coerceListToBindingValues()
  {
    List list = getList();
    for( int i = 0; i < list.size(); i++ )
    {
      Object e = list.get( i );
      e = toBindingsValue( (T) e );
      list.set( i, e );
    }
  }

  default List<T> coerceListToComplexValues()
  {
    return (List) getList().stream()
      .map( e -> coerce( e, getFinalComponentType() ) )
      .collect( Collectors.toList() );
  }
  
  default Object coerce( Object value, Class type )
  {
    return RuntimeMethods.coerceFromBindingsValue( value, type );
  }

  @Override
  default void replaceAll( UnaryOperator<T> operator )
  {
    getList().replaceAll( e -> toBindingsValue( operator.apply( (T) coerce( e, getFinalComponentType() ) ) ) );
  }

  @Override
  default void sort( Comparator<? super T> c )
  {
    List<T> cList = coerceListToComplexValues();
    cList.sort( c );
    List<Object> bList = toBindings( cList );
    for( int i = 0; i < bList.size(); i++ )
    {
      getList().set( i, bList.get( i ) );
    }
  }

  @Override
  default Spliterator<T> spliterator()
  {
    return coerceListToComplexValues().spliterator();
  }

  @Override
  default boolean removeIf( Predicate<? super T> filter )
  {
    List<T> cList = coerceListToComplexValues();
    boolean result = cList.removeIf( filter );
    if( result )
    {
      List<Object> bList = toBindings( cList );
      getList().clear();
      getList().addAll( bList );
    }
    return result;
  }

  @Override
  default Stream<T> stream()
  {
    return coerceListToComplexValues().stream();
  }

  @Override
  default Stream<T> parallelStream()
  {
    return coerceListToComplexValues().parallelStream();
  }

  @Override
  default void forEach( Consumer<? super T> action )
  {
    coerceListToComplexValues().forEach( action );
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
    return coerceListToComplexValues().contains( o );
  }

  @Override
  default Iterator<T> iterator()
  {
    return new Iterator<T>() {

      Iterator<T> _delegate = coerceListToComplexValues().iterator();
      Iterator _actual = getList().iterator();

      @Override
      public boolean hasNext()
      {
        return _delegate.hasNext();
      }

      @Override
      public T next()
      {
        _actual.next();
        return _delegate.next();
      }

      @Override
      public void remove()
      {
        _actual.remove();
      }
    };
  }

  @Override
  default Object[] toArray()
  {
    return coerceListToComplexValues().toArray();
  }

  @Override
  default <T1> T1[] toArray( T1[] a )
  {
    return coerceListToComplexValues().toArray( a );
  }

  @Override
  default boolean add( T t )
  {
    return getList().add( toBindingsValue( t ) );
  }

  default Object toBindingsValue( Object element )
  {
    Object value = element;
    if( value instanceof IBindingsBacked )
    {
      value = ((IBindingsBacked)value).getBindings();
    }
    else if( value instanceof IListBacked )
    {
      value = ((IListBacked)value).getList();
    }
    else
    {
      value = RuntimeMethods.coerceToBindingValue( value );
    }
    return value;
  }

  @Override
  default boolean remove( Object o )
  {
    Object bindingValue = toBindingsValue( (T) o );
    return getList().remove( bindingValue );
  }

  @Override
  default boolean containsAll( Collection<?> c )
  {
    return coerceListToComplexValues().containsAll( c );
  }

  @Override
  default boolean addAll( Collection<? extends T> c )
  {
    List<Object> all = toBindings( c );
    return getList().addAll( all );
  }

  @Override
  default boolean addAll( int index, Collection<? extends T> c )
  {
    List<Object> all = toBindings( c );
    return getList().addAll( index, (Collection<? extends T>) all );
  }

  default List<Object> toBindings( Collection<?> c )
  {
    return c.stream().map( e -> toBindingsValue( (T) e ) ).collect( Collectors.toList() );
  }

  @Override
  default boolean removeAll( Collection<?> c )
  {
    return getList().removeAll( toBindings( c ) );
  }

  @Override
  default boolean retainAll( Collection<?> c )
  {
    return getList().retainAll( toBindings( c ) );
  }

  @Override
  default void clear()
  {
    getList().clear();
  }

  @Override
  default T get( int index )
  {
    Object o = getList().get( index );
    return (T) coerce( o, getFinalComponentType() );
  }

  @Override
  default T set( int index, T element )
  {
    element = (T) toBindingsValue( element );
    T result = (T) getList().set( index, element );
    return result == null ? null : (T) coerce( result, getFinalComponentType() );
  }

  @Override
  default void add( int index, T element )
  {
    Object bindingValue = toBindingsValue( element );
    getList().add( index, (T) bindingValue );
  }

  @Override
  default T remove( int index )
  {
    Object bindingValue = getList().remove( index );
    return bindingValue == null ? null : (T) coerce( bindingValue, getFinalComponentType() );
  }

  @Override
  default int indexOf( Object o )
  {
    o = toBindingsValue( o );
    return getList().indexOf( o );
  }

  @Override
  default int lastIndexOf( Object o )
  {
    o = toBindingsValue( o );
    return getList().lastIndexOf( o );
  }

  @Override
  default ListIterator<T> listIterator()
  {
    return new ListIterator<T>()
    {
      ListIterator<T> _delegate = coerceListToComplexValues().listIterator();
      ListIterator _actual = getList().listIterator();

      @Override
      public boolean hasNext()
      {
        return _delegate.hasNext();
      }

      @Override
      public T next()
      {
        _actual.next();
        return _delegate.next();
      }

      @Override
      public boolean hasPrevious()
      {
        return _delegate.hasPrevious();
      }

      @Override
      public T previous()
      {
        _actual.previous();
        return _delegate.previous();
      }

      @Override
      public int nextIndex()
      {
        _actual.nextIndex();
        return _delegate.nextIndex();
      }

      @Override
      public int previousIndex()
      {
        _actual.previousIndex();
        return _delegate.previousIndex();
      }

      @Override
      public void remove()
      {
        _actual.remove();
        _delegate.remove();
      }

      @Override
      public void set( T t )
      {
        _actual.set( toBindingsValue( t ) );
        _delegate.set( t );
      }

      @Override
      public void add( T t )
      {
        _actual.add( toBindingsValue( t ) );
        _delegate.add( t );
      }
    };
  }

  @Override
  default ListIterator<T> listIterator( int index )
  {
    return new ListIterator<T>()
    {
      ListIterator<T> _delegate = coerceListToComplexValues().listIterator( index );
      ListIterator _actual = getList().listIterator( index );

      @Override
      public boolean hasNext()
      {
        return _delegate.hasNext();
      }

      @Override
      public T next()
      {
        _actual.next();
        return _delegate.next();
      }

      @Override
      public boolean hasPrevious()
      {
        return _delegate.hasPrevious();
      }

      @Override
      public T previous()
      {
        _actual.previous();
        return _delegate.previous();
      }

      @Override
      public int nextIndex()
      {
        _actual.nextIndex();
        return _delegate.nextIndex();
      }

      @Override
      public int previousIndex()
      {
        _actual.previousIndex();
        return _delegate.previousIndex();
      }

      @Override
      public void remove()
      {
        _actual.remove();
        _delegate.remove();
      }

      @Override
      public void set( T t )
      {
        _actual.set( toBindingsValue( t ) );
        _delegate.set( t );
      }

      @Override
      public void add( T t )
      {
        _actual.add( toBindingsValue( t ) );
        _delegate.add( t );
      }
    };
  }

  @Override
  default List<T> subList( int fromIndex, int toIndex )
  {
    return (List<T>) getList().subList( fromIndex, toIndex ).stream()
      .map( e -> coerce( e, getFinalComponentType() ) )
      .collect( Collectors.toList() );
  }
}
