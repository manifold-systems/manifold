/*
 * Copyright (c) 2022 - Manifold Systems LLC
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

package manifold.tuple;

import junit.framework.TestCase;
import manifold.ext.rt.api.Structural;
import manifold.tuple.nested.DifferentPackage;
import manifold.ext.rt.api.auto;
import manifold.tuple.rt.api.Tuple;
import manifold.tuple.rt.api.TupleItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertNotEquals;

public class BasicTest extends TestCase
{
  public void testBasic()
  {
    Foo foo = new Foo();
    assertEquals( 5, foo.explicitTupleConstruction().myField );
    Abc abc = new Abc();
    assertEquals( 5, abc.callMethod().myField );

    assertEquals( 5, foo.callHere().myField );
  }

  public void testLocal()
  {
    Foo foo = new Foo();
    auto tuple = foo.explicitTupleConstruction();
    assertEquals( 5, tuple.myField );
  }

  public void testIndirectReturn()
  {
    Foo foo = new Foo();
    auto res = foo.indirectReturn();
    assertEquals( 5, res.myField );
  }

  public void testNonlabeledMultipleReturn()
  {
    Foo foo = new Foo();
    auto res = foo.nonlabeledMultiReturn();
    assertEquals( "Helga", res.name );
    assertEquals( 2, res.age );
  }

  public void testLabeledMultipleReturn()
  {
    Foo foo = new Foo();
    auto res = foo.labeledMultiReturn();
    assertEquals( "Helga", res.Name );
    assertEquals( 2, res.Age );
  }

  public void testFromNonlabeledTupleExpression()
  {
    Foo foo = new Foo();
    auto res = foo.fromNonlabeledTupleExpression();
    assertEquals( "Helga", res.name );
    assertEquals( 2, res.age );
  }

  public void testFromLabeledTupleExpression()
  {
    Foo foo = new Foo();
    auto res = foo.fromLabeledTupleExpression();
    assertEquals( "Helga", res.Name );
    assertEquals( 2, res.Age );
  }

  public void testFromGenericTupleExpression()
  {
    Foo foo = new Foo();
    auto tuples = foo.fromGenericTupleExpression();
    assertEquals( 10, tuples.size() );
    int i = 0;
    for( auto tuple : tuples )
    {
      assertEquals( "Helga" + (i++), tuple.name );
      assertEquals( 2, tuple.age );
    }
  }

  public void testRefCycle()
  {
    Foo foo = new Foo();
    auto res = foo.refCycle();
    assertEquals( "Helga", res.Name );
    assertEquals( 2, res.Age );
  }

  public void testTxResults()
  {
    List<Person> people = new ArrayList<Person>() {{
      add( new Person( "Helga", 2 ) );
      add( new Person( "Natalie", 22 ) );
    }};
    Foo foo = new Foo();
  }

  public void testAutoRecursion()
  {
    Foo foo = new Foo();
    assertEquals( 5, foo.fib( 5 ) );
  }

//  public void testHeadRecursion()
//  {
//    Foo foo = new Foo();
//    auto res = foo.headRecursion( 5 );
//    res.append( "5" );
//    assertEquals( "     5", res.toString() );
//  }

  public void testOverrideFromDifferentPackage()
  {
    Foo foo = new DifferentPackage();
    auto result = foo.labeledMultiReturn();
    assertEquals( "Natalie", result.Name );
    assertEquals( 22, result.Age );
  }

  public void testAutoFieldTuple()
  {
    FieldAutos fieldAutos = new FieldAutos();
    assertEquals( "foo", fieldAutos._myTuple.foo );
    assertEquals( "bar", fieldAutos._myTuple.bar );
  }

  public void testAutoField()
  {
    FieldAutos fieldAutos = new FieldAutos();
    assertEquals( 2, fieldAutos._myList.size() );
    assertEquals( new ArrayList<String>() {{add("hi"); add("bye");}}, fieldAutos._myList );
    assertEquals( "bar", fieldAutos._myTuple.bar );
  }

  public void testTupleEquals()
  {
    LocalDate now = LocalDate.now();
    auto t1 = (now, "Hello", 5);
    auto t2 = (now, "Hello", 5);
    assertEquals( t1.hashCode(), t2.hashCode() );
    assertEquals( t1, t2 );
    auto t3 = (now, "Hello", 1);
    assertEquals( t1.getClass(), t3.getClass() );
    assertNotEquals( t1.hashCode(), t3.hashCode() );
    assertNotEquals( t1, t3 );
  }

  public void testIterable()
  {
    auto x = (name: "Scott", age: 20);
    List<Object> stuff = new ArrayList<>();
    for( TupleItem item: x )
    {
      stuff.add( item.getName() );
      stuff.add( item.getValue() );
    }
    assertEquals( 4, stuff.size() );
    assertTrue( stuff.contains( "name" ) );
    assertTrue( stuff.contains( "age" ) );
    assertTrue( stuff.contains( "Scott" ) );
    assertTrue( stuff.contains( 20 ) );
  }
  
  public void testNullReturn()
  {
    assertNull( nullTest( null ) );
    assertEquals( "foo", nullTest( LocalDate.now() ) );
    assertEquals( "hi", nullTest( LocalDate.of( 1999, 4, 9 ) ) );
    assertNull( nullTest( LocalDate.of( 2000, 4, 9 ) ) );
  }
  auto nullTest( LocalDate date )
  {
    if( date == null )
    {
      return null;
    }
    if( date.equals( LocalDate.now() ) )
    {
      return "foo";
    }
    if( date.equals( LocalDate.of( 1999, 4, 9 ) ) )
    {
      return "hi";
    }
    return null;
  }

  public void testLUB()
  {
    auto auto = lubTest( 0 );
    assertEquals( "hi", auto.iterator().next() );
  }
  auto lubTest( int i )
  {
    if( i == 0 )
    {
      return Arrays.asList( "hi", "bye" );
    }
    else if( i < 0 )
    {
      return new HashSet<String>() {{ add( "a" ); add( "b" ); }};
    }
    else
    {
      return Arrays.asList( "dog", "cat" );
    }
  }
  public void testLUB2()
  {
    auto res = lubTest2( 0 );
    CharSequence result = res;
    assertEquals( "hi", result.toString() );

    auto res2 = lubTest2( -1 );
    result = res2;
    assertEquals( "sb", result.toString() );

    auto res3 = lubTest2( 1 );
    result = res3;
    assertEquals( "bye", result.toString() );
  }
  auto lubTest2( int i )
  {
    if( i == 0 )
    {
      return "hi";
    }
    else if( i < 0 )
    {
      return new StringBuilder( "sb" );
    }
    else
    {
      return "bye";
    }
  }

  public void testCopy()
  {
    auto t = (name: "Scott", age: 100);
    auto t2 = t.copy();
    assertSame(t.name, t2.name);
    assertEquals("Scott", t2.name);
    assertEquals(100, t.age);
    t2.name = "Bob";
    assertEquals("Bob", t2.name);
    assertEquals("Scott", t.name);
    assertEquals(100, t.age);
  }

  public void testTupleInterface()
  {
    auto t = (name: "Scott", age: 100);
    Tuple result = hi( t );
    assertSame( result, t );
  }
  private auto hi( Tuple t )
  {
    return t;
  }

  public void testGenericValues()
  {
    ArrayList<String> list = new ArrayList<String>() {{ add("hi");}};
    MyGenericClass<String, List<String>, ArrayList<String>> l = new MyGenericClass<>( list );
    auto result= l.foo();
    CharSequence s = result.t.get( 0 );
    assertEquals( "hi", s );
    assertEquals( "bye", result.hi );

    auto result2 = l.foobar();
    List<? extends CharSequence> ls = result2.t.get( 0 );
    assertEquals( new ArrayList<String>() {{add("hi");}}, ls );
    assertEquals( "mmhmm", result2.hi );
  }
  static class MyGenericClass<E extends CharSequence, S extends List<? extends E>, T extends S>
  {
    private T _t;

    MyGenericClass( T t )
    {
      _t = t;
    }

    auto foo()
    {
      return (t: _t, hi: "bye");
    }

    auto foobar()
    {
      List<T> list = new ArrayList<>();
      list.add( _t );
      return (t: list, hi: "mmhmm");
    }
  }

  public void testMethodCallReceiver()
  {
    String noLabels = (1, 2).getClass().getTypeName();
    assertTrue( noLabels.contains( "manifold_tuple" ) );
    
    String withLabels = (one: 1, two: 2).getClass().getTypeName();
    assertTrue( noLabels.contains( "manifold_tuple" ) );

    int a = (a: 1, b: 2).a;
    assertEquals( 1, a );
    int b = (a: 1, b: 2).b;
    assertEquals( 2, b );
  }

  public void testTupleAsArgument()
  {
    auto t = passTuple( (a: 1, b: 2) );
    assertEquals( 1, t.a );
    assertEquals( 2, t.b );
  }
  public <T extends Tuple> T passTuple( T tuple )
  {
    return tuple;
  }

  public void testDefaultTupleNames()
  {
    LocalDate now = LocalDate.now();
    auto t1 = (1, "hi", 4 + 5, now, new BigDecimal( "3.14" ));
    assertEquals( 1, t1.item1 );
    assertEquals( "hi", t1.item2 );
    assertEquals( 9, t1.item3 );
    assertEquals( now, t1.now );
    assertEquals( new BigDecimal( "3.14" ), t1.item4 );

    assertEquals( "(item1: 1, item2: hi, item3: 9, item4: 3.14, now: " + now + ")", t1.toString() );
  }
  public void testTupleNameFromMethodName()
  {
    auto t1 = (getSDKVersion(), sdkVersion(), version(), SDK(), SDKVersion(), SdkVersion());
    List<String> labels = t1.orderedLabels();
    List<?> values = t1.orderedValues();
    int size = labels.size();
    for( int i = 0; i < size; i++ )
    {
      String label = labels.get( i );
      String value = (String)values.get( i );
      assertEquals( value, label );
    }
  }
  private String getSDKVersion()
  {
    return "sdkVersion";
  }
  private String sdkVersion()
  {
    return "version";
  }
  private String version()
  {
    return "version_2";
  }
  private String SDK()
  {
    return "sdk";
  }
  private String SDKVersion()
  {
    return "sdkVersion_2";
  }
  private String SdkVersion()
  {
    return "sdkVersion_3";
  }

  public void testTupleCastToStructuralInterface()
  {
    auto t = (name: "Bear", age:100);
    MyStructuralIface iface = (MyStructuralIface)t;
    assertEquals( "Bear", iface.getName() );
    iface.setName( "Bobcat" );
    assertEquals( "Bobcat", iface.getName() );
  }
  @Structural
  interface MyStructuralIface
  {
    String getName();
    void setName(String name);
  }

  static class Person
  {
    private final String _name;
    private final int _age;

    public Person( String name, int age )
    {
      _name = name;
      _age = age;
    }

    public String getName()
    {
      return _name;
    }

    public int getAge()
    {
      return _age;
    }
  }

//  public void testFromGenericTupleExpression()
//  {
//    Foo foo = new Foo();
//    List<tuple> res = foo.fromGenericTupleExpression();
//    assertEquals( "Helga0", res.get(0).name );
//    assertEquals( 2, res.get(0).age );
//  }
}
