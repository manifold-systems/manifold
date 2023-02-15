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

package manifold.ext.delegation.generics;

import junit.framework.TestCase;
import manifold.ext.delegation.rt.api.link;
import manifold.ext.delegation.rt.api.part;

import java.util.*;
import java.util.function.BiFunction;

public class MiscGenericTest extends TestCase
{
  public void testList()
  {
    MyList<String> l = new MyList<>();
    ArrayList<String> other = new ArrayList<String>(){{add("hi"); add("bye");}};
    l.add( "one" );
    l.addAll( other );
    assertEquals( 3, l.size() );
  }
  static class MyList<E> implements List<E>
  {
    @link List<E> l = new ArrayList<>();
    int times = 0;

    @Override
    public boolean add( E e )
    {
      times++;
      return l.add( e );
    }
  }

  static @part class MyMap<E> implements Map<String, E>
  {
    @link Map<String, E> m = new HashMap<>();

    @Override
    public E put( String key, E value )
    {
      System.out.println();
      return m.put( key, value );
    }

    // tests override of default with specified type var
    @Override
    public E computeIfPresent( String key, BiFunction<? super String, ? super E, ? extends E> remappingFunction )
    {
      return Map.super.computeIfPresent( key, remappingFunction );
    }
  }

  interface Foo<E> {
    E asdf( E e );
  }
  interface Bar<E> extends Foo<String> {
  }
  static class FooImpl<E> implements Bar<E> {
    @link Bar<E> foo;
  }
}
