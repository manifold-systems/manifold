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

package manifold.ext.rt.extensions.manifold.rt.api.Array;


import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.IndexedConsumer;
import manifold.ext.rt.api.Self;
import manifold.ext.rt.api.This;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Stream;

/**
 * Adds convenience methods to Java's array type. Uses @{@link Self} to enforce type-safety with both the array type and
 * the array component type.
 */
@Extension
public class ManArrayExt
{
  /**
   * Returns a fixed-size list backed by the specified array.  (Changes to
   * the returned list "write through" to the array.)  This method acts
   * as bridge between array-based and collection-based APIs, in
   * combination with {@link Collection#toArray}.  The returned list is
   * serializable and implements {@link RandomAccess}.
   *
   * @return a list view of the specified array
   */
  public static List<@Self(true) Object> toList( @This Object array )
  {
    if( array.getClass().getComponentType().isPrimitive() )
    {
      int len = Array.getLength( array );
      List<Object> list = new ArrayList<Object>( len );
      for( int i = 0; i < len; i++ )
      {
        list.add( Array.get( array, i ) );
      }
      return list;
    }
    return Arrays.asList( (Object[])array );
  }

  public static boolean isEmpty( @This Object array )
  {
    return Array.getLength( array ) == 0;
  }
  public static boolean isNullOrEmpty( @This Object array )
  {
    return array == null || isEmpty( array );
  }

  public static @Self(true) Object first( @This Object array )
  {
    return Array.get( array, 0 );
  }

  public static @Self(true) Object last( @This Object array )
  {
    return Array.get( array, Array.getLength( array ) - 1 );
  }

  public static @Self Object copy( @This Object array )
  {
    return copy( array, -1 );
  }

  /**
   * Copies this array's elements into a new array of the specified {@code newLength}. If {@code newLength} is < 0, the
   * new array's size matches this array, otherwise the new array is truncated or padded with {@code null} as {@code newLength}
   * differs from this array's length.
   *
   * @param newLength The length of the new array. If < 0, the resulting array's length matches this array, otherwise the
   *                  resulting array is truncated or padded with {@code null} accordingly.
   * @return A newly allocated array with elements corresponding with this array.
   */
  public static @Self Object copy( @This Object array, int newLength )
  {
    int length = Array.getLength( array );
    newLength = newLength < 0 ? length : newLength;
    Object dest = Array.newInstance( array.getClass().getComponentType(), newLength < 0 ? length : newLength );
    //noinspection SuspiciousSystemArraycopy
    System.arraycopy( array, 0, dest, 0, Math.min( length, newLength ) );
    return dest;
  }

  /**
   * Copies the elements of this array to a specified array, truncating or padding with nulls as needed.
   */
  public static @Self Object copyTo( @This Object array, @Self Object to )
  {
    //noinspection SuspiciousSystemArraycopy
    System.arraycopy( array, 0, to, 0, Math.min( Array.getLength( array ), Array.getLength( to ) ) );
    return to;
  }

  /**
   * Copies a range of elements from this array to a newly allocated array. Note, if {@code to} < 0, the range contains
   * the remainder of this array's elements.
   *
   * @param from The start point of the range, inclusive.
   * @param to The endpoint of the range, exclusive. A negative value may be used as a convenience to use this array's endpoint.
   * @return A newly allocated array containing the specified range of elements from this array.
   */
  public static @Self Object copyRange( @This Object array, int from, int to )
  {
    to = to < 0 ? Array.getLength( array ) : to;

    int rangeLength = to - from;
    if( rangeLength < 0 )
    {
      throw new IllegalArgumentException( from + " > " + to );
    }

    Object dest = Array.newInstance( array.getClass().getComponentType(), rangeLength );
    //noinspection SuspiciousSystemArraycopy
    System.arraycopy( array, from, dest, 0, rangeLength );
    return dest;
  }

  /**
   * Copies a range of elements from this array to the {@code target} array. Note, if {@code to} < 0, the range contains
   * the remainder of this array's elements.
   *
   * @param from The starting point of the range, inclusive.
   * @param to The endpoint of the range, exclusive. A negative value may be used as a convenience to use this array's endpoint.
   * @return The {@code target} array containing the specified range of elements from this array.
   */
  public static @Self Object copyRangeTo( @This Object array, int from, int to, @Self Object target, int targetIndex )
  {
    to = to < 0 ? Array.getLength( array ) : to;

    int rangeLength = to - from;
    if( rangeLength < 0 )
    {
      throw new IllegalArgumentException( from + " > " + to );
    }

    //noinspection SuspiciousSystemArraycopy
    System.arraycopy( array, from, target, targetIndex, rangeLength );
    return target;
  }

  /**
   * Returns a sequential {@link Stream} with this array as its source. Note, this array is assumed to be unmodified
   * during use
   *
   * @return A {@code Stream} for the array
   */
  public static Stream<@Self(true) Object> stream( @This Object array )
  {
    primitiveCheck( array );
    return Arrays.stream( (Object[])array, 0, Array.getLength( array ) );
  }

  public static void forEach( @This Object array, IndexedConsumer<? super @Self(true) Object> action )
  {
    primitiveCheck( array );
    Objects.requireNonNull( action );
    Object[] objects = (Object[])array;
    for( int i = 0; i < objects.length; i++ )
    {
      Object e = objects[i];
      action.accept( i, e );
    }
  }

  public static Spliterator<@Self(true) Object> spliterator( @This Object array )
  {
    primitiveCheck( array );
    return Spliterators.spliterator( (Object[])array, Spliterator.ORDERED | Spliterator.IMMUTABLE );
  }

  private static void primitiveCheck( Object array )
  {
    Class<?> componentType = array.getClass().getComponentType();
    if( componentType.isPrimitive() )
    {
      throw new IllegalArgumentException(
        array + " has a primitive component type: " + array.getClass().getComponentType().getSimpleName() );
    }
  }

  public static int binarySearch( @This Object array, @Self(true) Object key )
  {
    return binarySearch( array, 0, Array.getLength( array ), key );
  }

  public static int binarySearch( @This Object array, int from, int to, @Self(true) Object key )
  {
    Class<?> componentType = array.getClass().getComponentType();
    if( componentType.isPrimitive() )
    {
      switch( componentType.getTypeName() )
      {
        case "byte":
          return Arrays.binarySearch( (byte[])array, from, to, ((Number)key).byteValue() );
        case "short":
          return Arrays.binarySearch( (short[])array, from, to, ((Number)key).shortValue() );
        case "int":
          return Arrays.binarySearch( (int[])array, from, to, ((Number)key).intValue() );
        case "long":
          return Arrays.binarySearch( (long[])array, from, to, ((Number)key).longValue() );
        case "float":
          return Arrays.binarySearch( (float[])array, from, to, ((Number)key).floatValue() );
        case "double":
          return Arrays.binarySearch( (double[])array, from, to, ((Number)key).doubleValue() );
        case "char":
          return Arrays.binarySearch( (char[])array, from, to, (char)key );
        default:
          throw new UnsupportedOperationException( "Binary search unsupported for: " + componentType );
      }
    }
    return Arrays.binarySearch( (Object[])array, from, to, key );
  }
  public static int binarySearch( @This Object array, @Self(true) Object key, Comparator<? super @Self(true) Object> comparator )
  {
    return Arrays.binarySearch( (Object[])array, key, comparator );
  }
  public static int binarySearch( @This Object array, int from, int to, @Self(true) Object key, Comparator<? super @Self(true) Object> comparator )
  {
    return Arrays.binarySearch( (Object[])array, from, to, key, comparator );
  }

  public static String toString( @This Object array )
  {
    if( array == null )
    {
      return "null";
    }

    Class<?> componentType = array.getClass().getComponentType();
    if( componentType.isPrimitive() )
    {
      switch( componentType.getTypeName() )
      {
        case "byte":
          return Arrays.toString( (byte[])array );
        case "short":
          return Arrays.toString( (short[])array );
        case "int":
          return Arrays.toString( (int[])array );
        case "long":
          return Arrays.toString( (long[])array );
        case "float":
          return Arrays.toString( (float[])array );
        case "double":
          return Arrays.toString( (double[])array );
        case "char":
          return Arrays.toString( (char[])array );
        case "boolean":
          return Arrays.toString( (boolean[])array );
        default:
          throw new IllegalStateException();
      }
    }
    else if( !componentType.isArray() )
    {
      return Arrays.toString( (Object[])array );
    }
    return Arrays.deepToString( (Object[])array );
  }

  public static int hashCode( @This Object array )
  {
    Class<?> componentType = array.getClass().getComponentType();
    if( componentType.isPrimitive() )
    {
      switch( componentType.getTypeName() )
      {
        case "byte":
          return Arrays.hashCode( (byte[])array );
        case "short":
          return Arrays.hashCode( (short[])array );
        case "int":
          return Arrays.hashCode( (int[])array );
        case "long":
          return Arrays.hashCode( (long[])array );
        case "float":
          return Arrays.hashCode( (float[])array );
        case "double":
          return Arrays.hashCode( (double[])array );
        case "char":
          return Arrays.hashCode( (char[])array );
        case "boolean":
          return Arrays.hashCode( (boolean[])array );
        default:
          throw new IllegalStateException();
      }
    }
    else if( !componentType.isArray() )
    {
      return Arrays.hashCode( (Object[])array );
    }
    return Arrays.deepHashCode( (Object[])array );
  }

  public static boolean equals( @This Object array, @Self Object that )
  {
    if( array == null && that == null )
    {
      return true;
    }
    if( array == null || that == null )
    {
      return false;
    }

    Class<?> componentType = array.getClass().getComponentType();
    Class<?> thatComponentType = that.getClass().getComponentType();
    if( componentType.isPrimitive() && componentType == thatComponentType )
    {
      switch( componentType.getTypeName() )
      {
        case "byte":
          return Arrays.equals( (byte[])array, (byte[])that );
        case "short":
          return Arrays.equals( (short[])array, (short[])that );
        case "int":
          return Arrays.equals( (int[])array, (int[])that );
        case "long":
          return Arrays.equals( (long[])array, (long[])that );
        case "float":
          return Arrays.equals( (float[])array, (float[])that );
        case "double":
          return Arrays.equals( (double[])array, (double[])that );
        case "char":
          return Arrays.equals( (char[])array, (char[])that );
        case "boolean":
          return Arrays.equals( (boolean[])array, (boolean[])that );
        default:
          throw new IllegalStateException();
      }
    }
    else if( !componentType.isArray() && !thatComponentType.isArray() )
    {
      return Arrays.equals( (Object[])array, (Object[])that );
    }
    else if( componentType.isArray() && thatComponentType.isArray() )
    {
      return Arrays.deepEquals( (Object[])array, (Object[])that );
    }
    return false;
  }
}