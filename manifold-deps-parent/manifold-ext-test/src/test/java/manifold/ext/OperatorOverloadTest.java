/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.ext;

import java.math.BigDecimal;
import manifold.ext.api.IComparableWith;
import org.junit.Test;


import static org.junit.Assert.assertEquals;

public class OperatorOverloadTest
{
  @Test
  public void testBigDecimalArithmetic()
  {
    BigDecimal bd1 = new BigDecimal( "1.2" );
    BigDecimal bd2 = new BigDecimal( "2.4" );

    assertEquals( new BigDecimal( "-1.2" ), -bd1 );
    assertEquals( new BigDecimal( "3.6" ), bd1 + bd2 );
    assertEquals( new BigDecimal( "-1.2" ), bd1 - bd2 );
    assertEquals( new BigDecimal( "2.88" ), bd1 * bd2 );
    assertEquals( new BigDecimal( "0.5" ), bd1 / bd2 );
    assertEquals( new BigDecimal( "1.2" ), bd1 % bd2 );
  }

  @Test
  public void blah()
  {
    Fuzz<String> bar = new Fuzz<>("2");
    Fuzz<String> baz = new Fuzz<>("3");
    String s = (bar + baz) * "foo";
    assertEquals( "23foo", s );
    assertEquals( false, bar > baz );
    assertEquals( true, bar < baz );
  }

  Integer asdf( Fuzz<String> x, Fuzz<String> y )
  {
    return x * y;
  }
  static class Fuzz<T extends Comparable<T>> implements IComparableWith<Fuzz<T>>
  {
    T _s;
    public Fuzz( T s )
    {
      _s = s;
    }

    public String add( Fuzz<T> op )
    {
      return _s.toString() + op._s.toString();
    }

    public Integer multiply( Fuzz<T> op )
    {
      return 2;
    }

    private void foo(T t){}

    @Override
    public int compareTo( Fuzz<T> o )
    {
      return _s.compareTo( o._s );
    }
  }
}
