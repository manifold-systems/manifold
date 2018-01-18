package manifold.util;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.List;

public class Stack<T> implements Iterable<T>
{
  private final ArrayList<T> _list;

  public Stack()
  {
    _list = new ArrayList<T>();
  }

  public Stack( Stack<T> source )
  {
    _list = new ArrayList<T>( source._list );
  }

  public Stack( ArrayList<T> list )
  {
    _list = list;
  }

  public boolean push( T item )
  {
    return _list.add( item );
  }

  public void insert( T item, int iPos )
  {
    _list.add( iPos, item );
  }

  public T pop()
  {
    if( isEmpty() )
    {
      throw new EmptyStackException();
    }
    return _list.remove( size() - 1 );
  }

  public T peek()
  {
    if( isEmpty() )
    {
      throw new EmptyStackException();
    }
    return _list.get( size() - 1 );
  }

  public T getBase()
  {
    if( isEmpty() )
    {
      throw new EmptyStackException();
    }
    return _list.get( 0 );
  }

  public boolean contains( T obj )
  {
    return _list.contains( obj );
  }

  public Iterator<T> iterator()
  {
    return _list.iterator();
  }

  public T get( int i )
  {
    return _list.get( i );
  }

  public int indexOf( T o )
  {
    return _list.indexOf( o );
  }

  public void clear()
  {
    _list.clear();
  }

  public int size()
  {
    return _list.size();
  }

  public boolean isEmpty()
  {
    return _list.isEmpty();
  }

  @SuppressWarnings({"RedundantIfStatement"})
  public boolean equals( Object o )
  {
    if( this == o )
    {
      return true;
    }
    if( o == null || getClass() != o.getClass() )
    {
      return false;
    }

    Stack stack = (Stack)o;

    if( !_list.equals( stack._list ) )
    {
      return false;
    }

    return true;
  }

  public int hashCode()
  {
    return _list.hashCode();
  }

  public List<T> toList()
  {
    return new ArrayList<T>( _list );
  }

}
