/*
 * Copyright (c) 2023 - Manifold Systems LLC
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

package manifold.ext.delegation;

import junit.framework.TestCase;
import manifold.ext.delegation.rt.api.link;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ScratchTest extends TestCase
{
  public void testSimple() throws Exception
  {
    DelegatingClass<String> c = new DelegatingClass( "hi" );

    String res = c.call();
    assertEquals( "hi", res );

    c.add( "hi" );
    assertEquals( "hi", c.get( 0 ) );
  }

  interface Duplicate<E> extends Callable<E>
  {
    E poo(int i);
    E set(int index, E element); // overlaps List#set(int, E)
  }

  static class DelegatingClass<T> implements Callable<T>, List<T>
  {
    @link Callable<T> runner;
    @link List<T> list = new ArrayList<T>();
//    @link Duplicate<T> dup = new DuplicateImpl<>();

    public DelegatingClass( T t )
    {
      runner = new CallableFacet<>( t );
    }
    
    @Override
    public boolean add( T t )
    {
      return list.add( t );
    }

  }

//  private static class DuplicateImpl<E> implements Duplicate<E>
//  {
//    @Override
//    public E poo( int i )
//    {
//      return null;
//    }
//
//    @Override
//    public E set( int index, E element )
//    {
//      return null;
//    }
//  }

  private static class CallableFacet<T> implements Callable<T>
  {
    private final T _t;

    public CallableFacet( T t )
    {
      _t = t;
    }

    @Override
    public T call()
    {
      return _t;
    }
  }
}
