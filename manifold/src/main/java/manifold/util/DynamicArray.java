package manifold.util;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;

public class DynamicArray<E> extends AbstractList<E> implements List<E>, RandomAccess
{
  public static final DynamicArray EMPTY = new DynamicArray( 0 );

  /**
   * The array buffer into which the elements of the ArrayList are stored.
   * The capacity of the ArrayList is the length of this array buffer.
   */
  public transient Object[] data;

  /**
   * The size of the ArrayList (the number of elements it contains).
   *
   * @serial
   */
  public int size;

  /**
   * Constructs an empty list with the specified initial capacity.
   *
   * @param initialCapacity the initial capacity of the list
   *
   * @throws IllegalArgumentException if the specified initial capacity
   *                                  is negative
   */
  public DynamicArray( int initialCapacity )
  {
    super();
    if( initialCapacity < 0 )
    {
      throw new IllegalArgumentException( "Illegal Capacity: " +
                                          initialCapacity );
    }
    this.data = new Object[initialCapacity];
  }

  /**
   * Constructs an empty list with an initial capacity of ten.
   */
  public DynamicArray()
  {
    this( 10 );
  }

  /**
   * Constructs a list containing the elements of the specified
   * collection, in the order they are returned by the collection's
   * iterator.
   *
   * @param c the collection whose elements are to be placed into this list
   *
   * @throws NullPointerException if the specified collection is null
   */
  public DynamicArray( Collection<? extends E> c )
  {
    data = c.toArray();
    size = data.length;
    // c.toArray might (incorrectly) not return Object[] (see 6260652)
    if( data.getClass() != Object[].class )
    {
      Object[] copy = new Object[size];
      System.arraycopy( data, 0, copy, 0, size );
      data = copy;
    }
  }

  protected DynamicArray( DynamicArray<E> source )
  {
    size = source.size;
    data = Arrays.copyOf( source.data, size );
    modCount = 0;
  }

  /**
   * Returns a shallow copy of this <tt>ArrayList</tt> instance.  (The
   * elements themselves are not copied.)
   *
   * @return a copy of this <tt>DynamicArray</tt> instance
   */
  public DynamicArray<E> copy()
  {
    return new DynamicArray<E>( this );
  }

  /**
   * Trims the capacity of this <tt>ArrayList</tt> instance to be the
   * list's current size.  An application can use this operation to minimize
   * the storage of an <tt>ArrayList</tt> instance.
   */
  public void trimToSize()
  {
    modCount++;
    int oldCapacity = data.length;
    if( size < oldCapacity )
    {
      data = Arrays.copyOf( data, size );
    }
  }

  /**
   * Increases the capacity of this <tt>ArrayList</tt> instance, if
   * necessary, to ensure that it can hold at least the number of elements
   * specified by the minimum capacity argument.
   *
   * @param minCapacity the desired minimum capacity
   */
  public void ensureCapacity( int minCapacity )
  {
    modCount++;
    int oldCapacity = data.length;
    if( minCapacity > oldCapacity )
    {
      Object oldData[] = data;
      int newCapacity = (oldCapacity * 3) / 2 + 1;
      if( newCapacity < minCapacity )
      {
        newCapacity = minCapacity;
      }
      // minCapacity is usually close to size, so this is a win:
      data = Arrays.copyOf( data, newCapacity );
    }
  }

  /**
   * Returns the number of elements in this list.
   *
   * @return the number of elements in this list
   */
  public int size()
  {
    return size;
  }

  /**
   * Returns <tt>true</tt> if this list contains no elements.
   *
   * @return <tt>true</tt> if this list contains no elements
   */
  public boolean isEmpty()
  {
    return size == 0;
  }

  /**
   * Returns <tt>true</tt> if this list contains the specified element.
   * More formally, returns <tt>true</tt> if and only if this list contains
   * at least one element <tt>e</tt> such that
   * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
   *
   * @param o element whose presence in this list is to be tested
   *
   * @return <tt>true</tt> if this list contains the specified element
   */
  public boolean contains( Object o )
  {
    return indexOf( o ) >= 0;
  }

  /**
   * Returns the index of the first occurrence of the specified element
   * in this list, or -1 if this list does not contain the element.
   * More formally, returns the lowest index <tt>i</tt> such that
   * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
   * or -1 if there is no such index.
   */
  public int indexOf( Object o )
  {
    if( o == null )
    {
      for( int i = 0; i < size; i++ )
      {
        if( data[i] == null )
        {
          return i;
        }
      }
    }
    else
    {
      for( int i = 0; i < size; i++ )
      {
        if( o.equals( data[i] ) )
        {
          return i;
        }
      }
    }
    return -1;
  }

  /**
   * Returns the index of the last occurrence of the specified element
   * in this list, or -1 if this list does not contain the element.
   * More formally, returns the highest index <tt>i</tt> such that
   * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
   * or -1 if there is no such index.
   */
  public int lastIndexOf( Object o )
  {
    if( o == null )
    {
      for( int i = size - 1; i >= 0; i-- )
      {
        if( data[i] == null )
        {
          return i;
        }
      }
    }
    else
    {
      for( int i = size - 1; i >= 0; i-- )
      {
        if( o.equals( data[i] ) )
        {
          return i;
        }
      }
    }
    return -1;
  }

  /**
   * Returns an array containing all of the elements in this list
   * in proper sequence (from first to last element).
   * <p/>
   * <p>The returned array will be "safe" in that no references to it are
   * maintained by this list.  (In other words, this method must allocate
   * a new array).  The caller is thus free to modify the returned array.
   * <p/>
   * <p>This method acts as bridge between array-based and collection-based
   * APIs.
   *
   * @return an array containing all of the elements in this list in
   * proper sequence
   */
  public Object[] toArray()
  {
    return Arrays.copyOf( data, size );
  }

  /**
   * Returns an array containing all of the elements in this list in proper
   * sequence (from first to last element); the runtime type of the returned
   * array is that of the specified array.  If the list fits in the
   * specified array, it is returned therein.  Otherwise, a new array is
   * allocated with the runtime type of the specified array and the size of
   * this list.
   * <p/>
   * <p>If the list fits in the specified array with room to spare
   * (i.e., the array has more elements than the list), the element in
   * the array immediately following the end of the collection is set to
   * <tt>null</tt>.  (This is useful in determining the length of the
   * list <i>only</i> if the caller knows that the list does not contain
   * any null elements.)
   *
   * @param a the array into which the elements of the list are to
   *          be stored, if it is big enough; otherwise, a new array of the
   *          same runtime type is allocated for this purpose.
   *
   * @return an array containing the elements of the list
   *
   * @throws ArrayStoreException  if the runtime type of the specified array
   *                              is not a supertype of the runtime type of every element in
   *                              this list
   * @throws NullPointerException if the specified array is null
   */
  public <T> T[] toArray( T[] a )
  {
    if( a.length < size )
    // Make a new array of a's runtime type, but my contents:
    {
      return (T[])Arrays.copyOf( data, size, a.getClass() );
    }
    System.arraycopy( data, 0, a, 0, size );
    if( a.length > size )
    {
      a[size] = null;
    }
    return a;
  }

  // Positional Access Operations

  /**
   * Returns the element at the specified position in this list.
   *
   * @param index index of the element to return
   *
   * @return the element at the specified position in this list
   *
   * @throws IndexOutOfBoundsException {@inheritDoc}
   */
  public E get( int index )
  {
    RangeCheck( index );

    return (E)data[index];
  }

  /**
   * Replaces the element at the specified position in this list with
   * the specified element.
   *
   * @param index   index of the element to replace
   * @param element element to be stored at the specified position
   *
   * @return the element previously at the specified position
   *
   * @throws IndexOutOfBoundsException {@inheritDoc}
   */
  public E set( int index, E element )
  {
    RangeCheck( index );

    E oldValue = (E)data[index];
    data[index] = element;
    return oldValue;
  }

  /**
   * Appends the specified element to the end of this list.
   *
   * @param e element to be appended to this list
   *
   * @return <tt>true</tt> (as specified by {@link Collection#add})
   */
  public boolean add( E e )
  {
    ensureCapacity( size + 1 );  // Increments modCount!!
    data[size++] = e;
    return true;
  }

  /**
   * Inserts the specified element at the specified position in this
   * list. Shifts the element currently at that position (if any) and
   * any subsequent elements to the right (adds one to their indices).
   *
   * @param index   index at which the specified element is to be inserted
   * @param element element to be inserted
   *
   * @throws IndexOutOfBoundsException {@inheritDoc}
   */
  public void add( int index, E element )
  {
    if( index > size || index < 0 )
    {
      throw new IndexOutOfBoundsException(
        "Index: " + index + ", Size: " + size );
    }

    ensureCapacity( size + 1 );  // Increments modCount!!
    System.arraycopy( data, index, data, index + 1,
                      size - index );
    data[index] = element;
    size++;
  }

  /**
   * Removes the element at the specified position in this list.
   * Shifts any subsequent elements to the left (subtracts one from their
   * indices).
   *
   * @param index the index of the element to be removed
   *
   * @return the element that was removed from the list
   *
   * @throws IndexOutOfBoundsException {@inheritDoc}
   */
  public E remove( int index )
  {
    RangeCheck( index );

    modCount++;
    E oldValue = (E)data[index];

    int numMoved = size - index - 1;
    if( numMoved > 0 )
    {
      System.arraycopy( data, index + 1, data, index,
                        numMoved );
    }
    data[--size] = null; // Let gc do its work

    return oldValue;
  }

  /**
   * Removes the first occurrence of the specified element from this list,
   * if it is present.  If the list does not contain the element, it is
   * unchanged.  More formally, removes the element with the lowest index
   * <tt>i</tt> such that
   * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>
   * (if such an element exists).  Returns <tt>true</tt> if this list
   * contained the specified element (or equivalently, if this list
   * changed as a result of the call).
   *
   * @param o element to be removed from this list, if present
   *
   * @return <tt>true</tt> if this list contained the specified element
   */
  public boolean remove( Object o )
  {
    if( o == null )
    {
      for( int index = 0; index < size; index++ )
      {
        if( data[index] == null )
        {
          fastRemove( index );
          return true;
        }
      }
    }
    else
    {
      for( int index = 0; index < size; index++ )
      {
        if( o.equals( data[index] ) )
        {
          fastRemove( index );
          return true;
        }
      }
    }
    return false;
  }

  /*
  * Private remove method that skips bounds checking and does not
  * return the value removed.
  */
  private void fastRemove( int index )
  {
    modCount++;
    int numMoved = size - index - 1;
    if( numMoved > 0 )
    {
      System.arraycopy( data, index + 1, data, index,
                        numMoved );
    }
    data[--size] = null; // Let gc do its work
  }

  /**
   * Removes all of the elements from this list.  The list will
   * be empty after this call returns.
   */
  public void clear()
  {
    modCount++;

    // Let gc do its work
    for( int i = 0; i < size; i++ )
    {
      data[i] = null;
    }

    size = 0;
  }

  /**
   * Appends all of the elements in the specified collection to the end of
   * this list, in the order that they are returned by the
   * specified collection's Iterator.  The behavior of this operation is
   * undefined if the specified collection is modified while the operation
   * is in progress.  (This implies that the behavior of this call is
   * undefined if the specified collection is this list, and this
   * list is nonempty.)
   *
   * @param c collection containing elements to be added to this list
   *
   * @return <tt>true</tt> if this list changed as a result of the call
   *
   * @throws NullPointerException if the specified collection is null
   */
  public boolean addAll( Collection<? extends E> c )
  {
    Object[] a = c.toArray();
    int numNew = a.length;
    ensureCapacity( size + numNew );  // Increments modCount
    System.arraycopy( a, 0, data, size, numNew );
    size += numNew;
    return numNew != 0;
  }

  /**
   * Inserts all of the elements in the specified collection into this
   * list, starting at the specified position.  Shifts the element
   * currently at that position (if any) and any subsequent elements to
   * the right (increases their indices).  The new elements will appear
   * in the list in the order that they are returned by the
   * specified collection's iterator.
   *
   * @param index index at which to insert the first element from the
   *              specified collection
   * @param c     collection containing elements to be added to this list
   *
   * @return <tt>true</tt> if this list changed as a result of the call
   *
   * @throws IndexOutOfBoundsException {@inheritDoc}
   * @throws NullPointerException      if the specified collection is null
   */
  public boolean addAll( int index, Collection<? extends E> c )
  {
    if( index > size || index < 0 )
    {
      throw new IndexOutOfBoundsException(
        "Index: " + index + ", Size: " + size );
    }

    Object[] a = c.toArray();
    int numNew = a.length;
    ensureCapacity( size + numNew );  // Increments modCount

    int numMoved = size - index;
    if( numMoved > 0 )
    {
      System.arraycopy( data, index, data, index + numNew,
                        numMoved );
    }

    System.arraycopy( a, 0, data, index, numNew );
    size += numNew;
    return numNew != 0;
  }

  /**
   * Removes from this list all of the elements whose index is between
   * <tt>fromIndex</tt>, inclusive, and <tt>toIndex</tt>, exclusive.
   * Shifts any succeeding elements to the left (reduces their index).
   * This call shortens the list by <tt>(toIndex - fromIndex)</tt> elements.
   * (If <tt>toIndex==fromIndex</tt>, this operation has no effect.)
   *
   * @param fromIndex index of first element to be removed
   * @param toIndex   index after last element to be removed
   *
   * @throws IndexOutOfBoundsException if fromIndex or toIndex out of
   *                                   range (fromIndex &lt; 0 || fromIndex &gt;= size() || toIndex
   *                                   &gt; size() || toIndex &lt; fromIndex)
   */
  protected void removeRange( int fromIndex, int toIndex )
  {
    modCount++;
    int numMoved = size - toIndex;
    System.arraycopy( data, toIndex, data, fromIndex,
                      numMoved );

    // Let gc do its work
    int newSize = size - (toIndex - fromIndex);
    while( size != newSize )
    {
      data[--size] = null;
    }
  }

  /**
   * Checks if the given index is in range.  If not, throws an appropriate
   * runtime exception.  This method does *not* check if the index is
   * negative: It is always used immediately prior to an array access,
   * which throws an ArrayIndexOutOfBoundsException if index is negative.
   */
  private void RangeCheck( int index )
  {
    if( index >= size )
    {
      throw new IndexOutOfBoundsException(
        "Index: " + index + ", Size: " + size );
    }
  }

}
