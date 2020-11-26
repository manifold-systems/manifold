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

import manifold.ext.rt.api.ComparableUsing;
import org.junit.Test;


import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class OperatorOverloadTest
{
  @Test
  public void testBinaryArithmetic()
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
  }

  @Test
  public void testCompoundAssignment()
  {
    Fuzz bar = new Fuzz( 3.0 );

    Fuzz fuzz = new Fuzz( 1.0 );
    fuzz += bar;
    assertTrue( new Fuzz( 4.0 ) == fuzz );
    assertTrue( new Fuzz( 7.0 ) == (fuzz += bar) );

    fuzz = new Fuzz( 5.0 );
    fuzz -= bar;
    assertTrue( new Fuzz( 2.0 ) == fuzz );
    assertTrue( new Fuzz( -1.0 ) == (fuzz -= bar) );

    fuzz = new Fuzz( 5.0 );
    fuzz *= bar;
    assertTrue( new Fuzz( 15.0 ) == fuzz );
    assertTrue( new Fuzz( 45.0 ) == (fuzz *= bar) );

    fuzz = new Fuzz( 45.0 );
    fuzz /= bar;
    assertTrue( new Fuzz( 15.0 ) == fuzz );
    assertTrue( new Fuzz( 5.0 ) == (fuzz /= bar) );

    Fuzz[] array = {new Fuzz( 5.0 )};
    array[0] %= bar;
    assertTrue( new Fuzz( 2.0 ) == array[0] );
    assertTrue( new Fuzz( 2.0 ) == (array[0] %= bar) );
  }

  @Test
  public void testArrayCompoundAssign()
  {
    Fuzz bar = new Fuzz(3.0);

    Fuzz[] array = {new Fuzz(1.0)};
    array[0] += bar;
    assertTrue( new Fuzz(4.0) == array[0] );
    assertTrue( new Fuzz(7.0) == (array[0] += bar) );
  }

  @Test
  public void testRelational()
  {
    Fuzz bar = new Fuzz(3.0);
    Fuzz baz = new Fuzz(12.0);

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

  @Test
  public void testComparableUsingComparable()
  {
    assertTrue( "A" < "B" );
    assertTrue( "A" <= "B" );
    assertTrue( "A" <= "A" );
    assertTrue( "B" > "A" );
    assertTrue( "B" >= "A" );
    assertTrue( "B" >= "B" );
  }

  @Test
  public void testIncDec()
  {
    Fuzz fuzz = new Fuzz( 7.0 );

    // inc/dec operators
    Fuzz postRes = fuzz--;
    assertTrue( new Fuzz(6) == fuzz );
    assertTrue( new Fuzz(7) == postRes );

    Fuzz preRes = --fuzz;
    assertTrue( new Fuzz(5) == fuzz );
    assertTrue( new Fuzz(5) == preRes );

    char a = fuzz[2]--;
    assertEquals( 'd', fuzz[2] );
    assertEquals( 'e', a );

    a = --fuzz[2];
    assertEquals( 'c', fuzz[2] );
    assertEquals( 'c', a );
  }

  @Test
  public void testArrayIncDec()
  {
    Fuzz[] array = {new Fuzz(7.0)};

    Fuzz postRes = array[0]--;
    assertTrue( new Fuzz(6) == array[0] );
    assertTrue( new Fuzz(7) == postRes );

    Fuzz preRes = --array[0];
    assertTrue( new Fuzz(5) == array[0] );
    assertTrue( new Fuzz(5) == preRes );
  }

  @Test
  public void testIndexOperator()
  {
    Fuzz fuzz = new Fuzz( 1.0 );

    // test index operator
    assertEquals( 'r', fuzz[1] );
    fuzz[1] = 'w';
    assertEquals( 'w', fuzz[1] );
  }

  @Test
  public void testIndexedOverloadNoIncDecOverload()
  {
    Indexed<Integer> indexed = new Indexed<>();
    indexed += 3;
    indexed += 4;
    int i = indexed[1]++;
    assertEquals( 4, i );
    Integer ii = indexed[1]++;
    assertEquals( (Integer)5, ii );
    i = ++indexed[1];
    assertEquals( 7, i );
    ii = ++indexed[1];
    assertEquals( (Integer)8, ii );
  }

  @Test
  public void testIndexedOverloadWithIncDecOverload()
  {
    Indexed<FooIncDec> indexed = new Indexed<>();
    indexed += new FooIncDec( 3 );
    indexed += new FooIncDec( 4 );
    FooIncDec i = indexed[1]++;
    assertEquals( new FooIncDec( 4 ), i );
    i = ++indexed[1];
    assertEquals( new FooIncDec( 6 ), i );
  }

  @Test
  public void testArrayWithIncDecOverload()
  {
    FooIncDec[] indexed = {new FooIncDec( 3 ), new FooIncDec( 4 )};
    FooIncDec i = indexed[1]++;
    assertEquals( new FooIncDec( 4 ), i );
    i = ++indexed[1];
    assertEquals( new FooIncDec( 6 ), i );
  }

  @Test
  public void testCastWithCompoundAssign()
  {
    // always cast rhs for the case where the original statement was a compound assign involving a primitive type
    // (manifold transforms a += b to a = a + b, so that we can simply use plus() to handle both addition and compound
    // assign addition, however:
    //   short a = 0;
    //   a += (byte)b;
    // blows up if we don't cast the rhs of the resulting
    // transformation:  a += (byte)b;  parse==>  a = a + (byte)b;  attr==>  a = (short) (a + (byte)b);

    short a = 0;
    a += (byte)1;
    assertEquals( 1, a );
  }

  static class FooIncDec
  {
    int _value;

    public FooIncDec( int value )
    {
      _value = value;
    }

    public FooIncDec inc()
    {
      return new FooIncDec( _value + 1 );
    }
    public FooIncDec dec()
    {
      return new FooIncDec( _value - 1 );
    }

    public boolean equals( Object o )
    {
      return o instanceof FooIncDec && ((FooIncDec)o)._value == _value;
    }
  }

  static class Indexed<T>
  {
    List<T> _list = new ArrayList<T>();

    public T get( int i )
    {
      return _list.get( i );
    }
    public T set( int i, T value )
    {
      // note, returning prev value, our bytecode for assign ops take care evaluating to the new value
      return _list.set( i, value );
    }

    public Indexed<T> plus( T value )
    {
      _list.add( value );
      return this;
    }
  }

  static class Fuzz implements ComparableUsing<Fuzz>
  {
    final double _value;
    final StringBuilder _name = new StringBuilder( "Fred" );

    public Fuzz( double value )
    {
      _value = value;
    }

    public Fuzz unaryMinus()
    {
      return new Fuzz( -_value );
    }

    public Fuzz inc()
    {
      return new Fuzz( _value + 1 );
    }
    public Fuzz dec()
    {
      return new Fuzz( _value - 1 );
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

    public char get( int index )
    {
      return _name.charAt( index );
    }

    public char set( int index, char value )
    {
      char prev = _name.charAt( index );
      _name.setCharAt( index, value );
      return prev; // note, returning prev value, our bytecode for assign ops take care evaluating to the new value
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

    @Override
    public String toString()
    {
      return "_value: " + _value + "\n" + 
             "_name: " + _name;
    }
  }
}
