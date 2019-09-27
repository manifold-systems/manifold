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

package manifold.science;

import java.util.ArrayList;
import java.util.List;
import manifold.ext.api.IComparableWith;
import manifold.science.util.Rational;
import org.junit.Test;


import static manifold.science.MetricScaleUnit.*;
import static org.junit.Assert.*;

public class TestMe
{
  @Test
  public void blah()
  {
    Fuzz<String> bar = new Fuzz<>("2");
    Fuzz<String> baz = new Fuzz<>("3");
    int s = (bar + baz) * bar;
    assertEquals( 46, s );
    assertEquals( false, bar > baz );
    assertEquals( true, bar < baz );

    List<Fuzz<String>> l = new ArrayList<>();
    l.add( bar + baz );

    Rational x = Rational.ONE;
    Rational y = Rational.get( 8 );
    Rational z = x * y;
    assertEquals( y, z );

//    int xx = 8;
//    double yy = 2xx;

    assertTrue( 2 + 10 k / 2 * 3 + 6 * 2k + 1 == 2 + 10 k / 2 * 3 + 6 * 2k + 1 );
    assertEquals( Rational.get( 27002 ), 1 + 10 k / 2 * 3 + 6 * 2k + 1 );

    assertEquals( Rational.get( 123 ), 123r );

//    Length length = 5 bar x;
//    int h = bar 5;
  }

  Integer asdf( Fuzz<String> x, Fuzz<String> y )
  {
    return x * y;
  }
  static class Fuzz<T extends Comparable<T>> extends Number implements IComparableWith<Fuzz<T>>
  {
    T _s;
    public Fuzz( T s )
    {
      _s = s;
    }

    public Fuzz<String> add( Fuzz<T> op )
    {
      return new Fuzz<>( _s.toString() + op._s.toString() );
    }

    public Integer multiply( Fuzz<T> op )
    {
      return Integer.parseInt( _s.toString() ) * Integer.parseInt( op._s.toString() );
    }

    public Fuzz<T> multiply( double op )
    {
      return new Fuzz<>( (T)String.valueOf( op ) );
    }

    private void foo(T t){}

    @Override
    public int compareTo( Fuzz<T> o )
    {
      return _s.compareTo( o._s );
    }

    @Override
    public int intValue()
    {
      return 0;
    }

    @Override
    public long longValue()
    {
      return 0;
    }

    @Override
    public float floatValue()
    {
      return 0;
    }

    @Override
    public double doubleValue()
    {
      return 0;
    }
  }
}
