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

import manifold.ext.api.ComparableUsing;
import org.junit.Test;


import static org.junit.Assert.*;

public class OperatorOverloadTest
{
  @Test
  public void testFuzz()
  {
    Fuzz bar = new Fuzz(3.0);
    Fuzz baz = new Fuzz(12.0);
    Fuzz boz = new Fuzz(5.0);

    assertTrue( new Fuzz(15.0) == bar + baz );
    assertTrue( new Fuzz(-9.0) == bar - baz );
    assertTrue( new Fuzz(36.0) == bar * baz );
    assertTrue( new Fuzz(4.0) == baz / bar );
    assertTrue( new Fuzz(2.0) == baz % boz );
    assertTrue( new Fuzz(-3.0) == -bar );

    assertFalse( bar == baz );
    assertTrue( bar != baz );
    assertFalse( bar > baz );
    assertFalse( bar >= baz );
    assertTrue( bar < baz );
    assertTrue( bar <= baz );
    assertFalse( -bar == bar );
    assertFalse( bar == -bar );
    assertTrue( -bar != bar );
    assertTrue( bar != -bar );
    assertTrue( -bar < bar );
    assertTrue( bar > -bar );
    assertTrue( -bar <= bar );
    assertTrue( bar >= -bar );
  }

  static class Fuzz implements ComparableUsing<Fuzz>
  {
    final double _value;

    public Fuzz( double value )
    {
      _value = value;
    }

    public Fuzz unaryMinus()
    {
      return new Fuzz( -_value );
    }

    public Fuzz plus( Fuzz op )
    {
      return new Fuzz( _value + op._value );
    }

    public Fuzz minus( Fuzz op )
    {
      return new Fuzz( _value - op._value );
    }

    public Fuzz times( Fuzz op )
    {
      return new Fuzz( _value * op._value);
    }

    public Fuzz div( Fuzz op )
    {
      return new Fuzz( _value / op._value);
    }

    public Fuzz rem( Fuzz op )
    {
      return new Fuzz( _value % op._value);
    }

    @Override
    public int compareTo( Fuzz o )
    {
      double diff = _value - o._value;
      return diff == 0 ? 0 : diff < 0 ? -1 : 1;
    }

    @Override
    public EqualityMode equalityMode()
    {
      return EqualityMode.CompareTo;
    }
  }
}
