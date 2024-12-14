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

import java.math.BigDecimal;


public class ScratchTest extends TestCase
{
  public void testBasic()
  {
    String res = optionalParams( (name:"Scott") );
    assertEquals( "Scott,100", res );
  }

  public void testOverloadGenericInferenceReturn()
  {
    String res = optionalParams( (id:"Scott", address:"FL") );
    assertEquals( "Scott,FL,null", res );

    res = optionalParams( (address:"FL") );
    assertEquals( "null,FL,null", res );

    res = optionalParams( (id:"Scott", address:"FL", phone:"555-555-5555") );
    assertEquals( "Scott,FL,555-555-5555", res );
  }

  public void testGenericClass()
  {
    Foo<String> stringFoo = new Foo<>();

    String res = stringFoo.bar( (name:"hi") );
    assertEquals( "hi,100", res );
    res = stringFoo.indirectBar( "hi" );
    assertEquals( "hi,100", res );

    String id = stringFoo.genericMethod( (id:"Scott") );
    assertEquals( "Scott", id );
    id = stringFoo.indirectGeneriMethod( "Scott" );
    assertEquals( "Scott", id );
  }

  public void testNull()
  {
    optionalParams( (name:null) );
  }

  public void testInterface()
  {
    Iface<Integer> iface = new IfaceImpl();
    Integer result = iface.getItem( (x:"1", y:2) );
    assertEquals( 6, result.intValue() );
  }

  public void testStaticInterfaceMethod()
  {
    Integer result = Iface.foo( (e:6) );
    assertEquals( 6, result.intValue() );

    // test non-labeled parameter
    result = Iface.foo( (6, s:"9") );
    assertEquals( 6, result.intValue() );
  }

  public void testStaticClassMethod()
  {
    Integer result = StaticMethod.foo( (e:6) );
    assertEquals( 6, result.intValue() );
  }

  public void testConstructor()
  {
    CtorTest<BigDecimal> result = new CtorTest<>( (t:new BigDecimal( 9 )) );
//    new CtorTest<>( new CtorTest.$constructor_t_foo<>(new BigDecimal( 9 ), "hi") );
    CtorTest<String> str = new CtorTest<>((foo:"foo"));
    assertEquals( "foo", str.getFoo() );
    str = new CtorTest<>(());
    assertEquals( "hi", str.getFoo() );
  }

  private String optionalParams( String name, int age =100 )
  {
    return name + "," + age;
  }

  private <E extends CharSequence> E optionalParams( E id =null, String address, String phone =null )
  {
    return (E)(id + "," + address + "," + phone);
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

  static class CtorTest<T>
  {
    private final T _t;
    private final String _foo;

    CtorTest( T t, String foo ="hi" )
    {
      _t = t;
      _foo = foo;
    }

    CtorTest( String foo ="hi" )
    {
      _t = null;
      _foo = foo;
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