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

package manifold.collections.test.extensions.java.util.Map;

import manifold.test.api.ExtensionManifoldTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static manifold.rt.api.util.Pair.and;

public class ManMapExtTest extends ExtensionManifoldTest
{
  public void testCoverage() {
   // testCoverage( ManMapExt.class);
  }

  public void testIndexedAssignment()
  {
    Map<String, Foo<String>> map = new HashMap<>();
    map["a"] = new Foo<>( "A" );
    map["b"] = new Foo<>( "B" );
    assertEquals( 2, map.size() );
    assertEquals( new Foo<>( "A" ), map["a"] );
    assertEquals( new Foo<>( "B" ), map["b"] );
  }

  public void testMapOf()
  {
    Map<String, Integer> map = Map.mapOf( "Moe" and 100, "Larry" and 107, "Curly" and 111 );
    assertEquals( 3, map.size() );
    assertEquals( (Integer)100, map.get( "Moe") );
    assertEquals( (Integer)107, map.get( "Larry") );
    assertEquals( (Integer)111, map.get( "Curly") );
    try
    {
      // test map is unmodifiable
      map.put( "x", 0 );
      fail();
    }
    catch( Exception ignore )
    {
    }
  }

  static class Foo<T>
  {
    T _value;

    Foo( T value )
    {
      _value = value;
    }

    @Override
    public boolean equals( Object o )
    {
      if( this == o ) return true;
      if( o == null || getClass() != o.getClass() ) return false;
      Foo<?> foo = (Foo<?>)o;
      return Objects.equals( _value, foo._value );
    }

    @Override
    public int hashCode()
    {
      return Objects.hash( _value );
    }
  }
}
