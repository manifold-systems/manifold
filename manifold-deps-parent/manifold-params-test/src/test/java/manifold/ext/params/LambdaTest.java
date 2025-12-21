/*
 * Copyright (c) 2021 - Manifold Systems LLC
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

package manifold.ext.params;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;


public class LambdaTest extends TestCase
{
  public void testLambdaArgs()
  {
    Foo foo = new Foo();

    String localHi = "hi";
    String hi = foo.testB( supp: () -> localHi );
    assertEquals( "hi", hi );
    Integer[] out = {3};

    String res = foo.testC(v: "hey", out: out);
    assertEquals( "hey", res );
    assertEquals( 2, out[0].intValue() );

    Optional<String> prompt = foo.prompt( message:"hey", m:s -> s );
    assertEquals( "heyhi", prompt.get() );

    String localHey = "hey";
    Optional<String> promptStatic = Foo.promptStatic( message:localHey, m:s -> s + localHey, l:new ArrayList<String>() );
    assertEquals( "hiheyhey", promptStatic.get() );
  }

  private String bub()
  {
    return "Bub";
  }

  static class Foo
  {
//    public <V> V testA( Supplier<V> supplier = null )
//    {
//      return supplier.get();
//    }

    public <V> V testB( Supplier<V> supp, boolean c = false )
    {
      return supp.get();
    }

    public <V> V testC( Supplier<Integer> supp = () -> 2, V v, Integer[] out )
    {
      out[0] = supp.get();
      return v;
    }

    private <T> Optional<T> prompt( String message, int i = 9, Function<String, T> m )
    {
      return Optional.of( m.apply( message + "hi" ) );
    }
    private static <T> Optional<T> promptStatic( String message, int i = 9, Function<String, T> m, List<T> l = new ArrayList<T>() )
    {
      return Optional.of( m.apply( "hi" + message ) );
    }
  }
}