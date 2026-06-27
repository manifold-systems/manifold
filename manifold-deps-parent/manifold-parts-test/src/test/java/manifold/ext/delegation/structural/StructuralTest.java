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

package manifold.ext.delegation.structural;

import junit.framework.TestCase;
import manifold.ext.delegation.rt.api.link;
import manifold.ext.rt.api.Structural;

import java.util.ArrayList;

public class StructuralTest extends TestCase
{
  public void testStructural()
  {
     LimitedList<String> limitedList = new MyLimitedList<>( (LimitedList<String>)new ArrayList<>() );
     limitedList.add( "hi" );
     assertTrue( limitedList.contains( "hi" ) );
     assertEquals( "hi", limitedList.get( 0 ) );
  }

  @Structural
  interface LimitedList<E>
  {
    boolean add(E e);
    E get( int index );
    boolean contains(Object e);
  }


  static class MyLimitedList<E> implements LimitedList<E>
  {
    @link LimitedList<E> _list;

    public MyLimitedList( LimitedList<E> list )
    {
      _list = list;
    }
  }
}
