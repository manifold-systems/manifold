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
import manifold.ext.rt.api.auto;

import java.math.BigDecimal;
import java.time.LocalTime;


public class ScratchTest extends TestCase
{
  public void testBasic()
  {
    String res = optionalParams1( name:"Scott" );
    assertEquals( "Scott,100", res );
    res = optionalParams1( name:"Scott" );
    assertEquals( "Scott,100", res );
  }

  public void testOverloadGenericInferenceReturn()
  {
    String res = optionalParams( id:"Scott", address:"FL" );
    assertEquals( "Scott,FL,null", res );

//    res = optionalParams( address:"FL" );
//    assertEquals( "null,FL,null", res );

    StringBuilder re = optionalParams2( id:new StringBuilder("Scott") );
    assertEquals( new StringBuilder("Scott").toString(), re.toString() );

    res = optionalParams( id:"Scott", address:"FL", phone:"555-555-5555" );
    assertEquals( "Scott,FL,555-555-5555", res );
    res = optionalParams( "Scott", address:"FL", phone:"555-555-5555" );
    assertEquals( "Scott,FL,555-555-5555", res );
    res = optionalParams2( null, address:"FL", phone:"555-555-5555" );
    assertEquals( "null,FL,555-555-5555", res );
    res = optionalParams2((null, address:"FL", phone:"555-555-5555")); // explicit tuple
    assertEquals( "null,FL,555-555-5555", res );
  }

  public void testGenericClass()
  {
    Foo<String> stringFoo = new Foo<>();

    String res = stringFoo.bar( name:"hi" );
    assertEquals( "hi,100", res );
    res = stringFoo.indirectBar( "hi" );
    assertEquals( "hi,100", res );

    String id = stringFoo.genericMethod( id:"Scott" );
    assertEquals( "Scott", id );
    id = stringFoo.indirectGeneriMethod( "Scott" );
    assertEquals( "Scott", id );
  }

  public void testNull()
  {
    optionalParams1( name:null );
  }

  public void testInterface()
  {
    Iface<Integer> iface = new IfaceImpl();
    Integer result = iface.getItem( x:"1", y:2 );
    assertEquals( 6, result.intValue() );
  }

  public void testStaticInterfaceMethod()
  {
    Integer result = Iface.foo( e: 6 );
    assertEquals( 6, result.intValue() );

    // test non-labeled parameter
    result = Iface.foo( 6, s: "9" );
    assertEquals( 6, result.intValue() );
  }

  public void testStaticClassMethod()
  {
    Integer result = StaticMethod.foo( e: 6 );
    assertEquals( 6, result.intValue() );
  }

  public void testConstructor()
  {
    long l = new CtorTest<>( t: new BigDecimal( 9 ) ).getT().longValueExact();
    assertEquals( 9L, l );

    int i = new CtorTest<>( t:9 ).getT();
    assertEquals( 9, i );

    auto result = new CtorTest<>( foo:"bar", t:8 );
    int result_t = result.getT();
    assertEquals( 8, result_t );
    assertEquals( "bar", result.getFoo() );

    //    auto strr = new CtorTest<>(t:5, fooo:"foo");
    CtorTest<String> str = new CtorTest<>("foo");
    assertEquals( "foo", str.getFoo() );
    assertNull( str.getT() );
    str = new CtorTest<>(t:"foo");
    assertEquals( "hello", str.getFoo() );
    assertEquals( "foo", str.getT() );
    str = new CtorTest<>("foo", "hi");
    assertEquals( "hi", str.getFoo() );
    assertEquals( "foo", str.getT() );
    str = new CtorTest<>("foo", foo:"hi");
    assertEquals( "hi", str.getFoo() );
    assertEquals( "foo", str.getT() );
    str = new CtorTest<>(t:"foo", foo:"hi");
    assertEquals( "hi", str.getFoo() );
    assertEquals( "foo", str.getT() );

    CtorTest<String> res = new CtorTest<>(foo:"foo");
    assertEquals( "foo", res.getFoo() );

    res = new CtorTest<>("foo");
    assertEquals( "foo", res.getFoo() );

    res = new CtorTest<>(());
    assertEquals( "hi", res.getFoo() );

    res = new CtorTest<>();
    assertEquals( "hi", res.getFoo() );
    /**/
    res = new CtorTest<>(5);
    assertEquals( "sb", res.getFoo() );
  }

  public void testTelescopingNoNames()
  {
    String result = optionalParams( "111 main st." );
    assertEquals( "null,111 main st.,null", result );

    result = optionalParams( "myId", "111 main st." );
    assertEquals( "myId,111 main st.,null", result );

    result = optionalParams( "myId", "111 main st.", "555-5555" );
    assertEquals( "myId,111 main st.,555-5555", result );

    CtorTest<String> ctorTest = new CtorTest<>();
    assertEquals( "hi", ctorTest.getFoo() );
  }

  public void testSomething()
  {
    String result = valueOf( new char[] {'a', 'b', 'c'} );
    assertEquals( "abc", result );

    result = valueOf( new char[] {'a', 'b', 'c'}, 1 );
    assertEquals( "bc", result );

    result = valueOf( data: new char[] {'a', 'b', 'c'}, count: 2 );
    assertEquals( "ab", result );

    result = valueOf( new char[] {'a', 'b', 'c'}, offset: 1, count: 1 );
    assertEquals( "b", result );
  }

  public void testInstanceMemberAccess()
  {
    InstanceMemberTest hi = new InstanceMemberTest( "hi" );
    String result = hi.testInstanceMemberAccess();
    assertEquals( "hi", result );
  }

  public String valueOf(char[] data,
                        int offset = 0,
                        int count = data.length - offset) {
    return String.valueOf( data, offset, count );
  }

  private String optionalParams1( String name, int age =100 )
  {
    return name + "," + age;
  }

  private <E extends CharSequence> E optionalParams( E id =null, String address, String phone =null )
  {
    return (E)(id + "," + address + "," + phone);
  }

  private <E extends CharSequence> E optionalParams2( E id =null )
  {
    return id;
  }

  private String optionalParams2(LocalTime time = LocalTime.now(), String address, String phone = null )
  {
    return (time + "," + address + "," + phone);
  }

  static class Foo<T extends CharSequence>
  {
    String indirectBar( String n )
    {
      return bar( (name:n) );
    }

    String bar( String name, int age =100 )
    {
      return name + "," + age;
    }

    <E extends CharSequence> E indirectGeneriMethod( E id )
    {
      return genericMethod( (id:id) );
    }

    <E extends CharSequence> E genericMethod( E id, String password =null )
    {
      return id;
    }
  }

  static class IfaceImpl implements Iface<Integer>
  {
    @Override
    public Integer getItem( String x, int y, String z )
    {
      return Integer.parseInt( x ) + Integer.parseInt( z ) + y;
    }
  }

  interface Iface<T>
  {
    T getItem( String x, int y, String z ="3" );

    static <E extends Number> E foo( E e, String s ="8" )
    {
      return e;
    }
  }

  static class StaticMethod
  {
    static <E extends Number> E foo( E e, String s ="8" )
    {
      return e;
    }
  }

  static class InstanceMemberTest
  {
    private String _data;

    InstanceMemberTest( String data )
    {
      _data = data;
    }

    String testInstanceMemberAccess( String foo = _data )
    {
      return foo;
    }
  }

  static class CtorTest<T>
  {
    private final T _t;
    private final String _foo;

    CtorTest( T t, String foo ="hello" )
    {
      _t = t;
      _foo = foo;
    }

    CtorTest( String foo ="hi" )
    {
      _t = null;
      _foo = foo;
    }

    CtorTest( int i, StringBuilder sb = new StringBuilder("sb") )
    {
      _t = null;
      _foo = sb.toString();
    }

    T getT()
    {
      return _t;
    }

    String getFoo()
    {
      return _foo;
    }
//    CtorTest($constructor_t_foo<T> f) {
//      this(f.t, f.foo);
//    }
//    static class $constructor_t_foo<T> {
//      $constructor_t_foo(T t, String foo) {
//        this.t = t;
//        this.foo = foo;
//      }
//      T t;
//      String foo;
//    }
  }

}