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


public class InheritanceTest extends TestCase
{
  public void testInherited()
  {
    BaseClass baseClass = new SubClass();
    assertEquals( "SubClass: BaseClass: Scott,90", baseClass.optionalParams( name:"Scott", age:90 ) );
    assertEquals( "SubClass: BaseClass: null,90", baseClass.optionalParams( null, age:90 ) );
    assertEquals( "SubClass: BaseClass: Scott,100", baseClass.optionalParams( "Scott" ) );
    assertEquals( "SubClass: BaseClass: base", baseClass.testme( name:"base" ) );
    assertEquals( "SubClass: BaseClass: default", baseClass.testme() );
    assertEquals( "SubClass: BaseClass: base", baseClass.testmeDefaultImpl( name:"base" ) );
    assertEquals( "SubClass: BaseClass: default", baseClass.testmeDefaultImpl() );

    SubClass subClass = (SubClass)baseClass;
    assertEquals( "SubClass: BaseClass: Scott,90", subClass.optionalParams( name:"Scott", age:90 ) );
    assertEquals( "SubClass: BaseClass: Scott,100", subClass.optionalParams( name:"Scott" ) );
    assertEquals( "SubClass: BaseClass: Scott,100", subClass.optionalParams( "Scott" ) );
    assertEquals( "SubClass: BaseClass: sub", baseClass.testme( name:"sub" ) );
    assertEquals( "SubClass: BaseClass: default", baseClass.testme() );
    assertEquals( "SubClass: BaseClass: sub", baseClass.testmeDefaultImpl( name:"sub" ) );
    assertEquals( "SubClass: BaseClass: default", baseClass.testmeDefaultImpl() );
  }

  public void testAnon()
  {
    MyInterface anon =
      new MyInterface()
      {
        @Override
        public String testme( String name )
        {
          return "Anon: " + name;
        }

        @Override
        public String testmeDefaultImpl( String name )
        {
          return "Anon: " + MyInterface.super.testmeDefaultImpl( name );
        }
//## todo: handle default value overrides in anonymous classes, see ParamsProcessor.Analyze_Finish
//        @Override
//        public String testmeDefaultImpl( String name = "overrideDefault" )
//        {
//          return "Anon: " + MyInterface.super.testmeDefaultImpl( name );
//        }
      };
    assertEquals( "Anon: anon", anon.testme( name: "anon" ) );
    assertEquals( "Anon: default", anon.testme() );
    assertEquals( "Anon: anon", anon.testmeDefaultImpl( name: "anon" ) );
    assertEquals( "Anon: default", anon.testmeDefaultImpl() );
  }

  interface MyInterface
  {
    String testme( String name = "default" );
    default String testmeDefaultImpl( String name = "default" ) {return name;}
  }

  static class BaseClass implements MyInterface
  {
    String optionalParams( String name, int age =100 )
    {
      return "BaseClass: " + name + "," + age;
    }

    @Override
    public String testme( String name )
    {
      return "BaseClass: " + name;
    }

    @Override
    public String testmeDefaultImpl( String name )
    {
      return "BaseClass: " + MyInterface.super.testmeDefaultImpl( name );
    }
  }

  static class SubClass extends BaseClass
  {
    String optionalParams( String name, int age, int hi = 9 )
    {
      return "SubClass: " + super.optionalParams( name, age );
    }

    @Override
    public String testme( String name )
    {
      return "SubClass: " + super.testme( name );
    }

    @Override
    public String testmeDefaultImpl( String name )
    {
      return "SubClass: " + super.testmeDefaultImpl( name );
    }
  }

  public void testOverrideOptParamWithAdditionalOptParam()
  {
    Base base = new Sub();
    assertEquals( "a:hi b:8 c:8", base.func( "hi", 8 ) );
    assertEquals( "a:hi b:-1 c:-1", base.func( "hi" ) );
    assertEquals( "a:hey b:1 c:0", ((Sub)base).func( "hey", 1, c:0 ) );
  }

  public void testOverrideOptParamValues()
  {
    // base sanity check
    Base base = new Base();

    assertEquals( "a:hi b:8", base.func2( "hi", 8 ) );
    assertEquals( "a:hi b:0", base.func2( "hi" ) );

    assertEquals( "a:hi b:8", base.func3( "hi", 8 ) );
    assertEquals( "a:hi b:0", base.func3( "hi" ) );

    // sub sanity check
    Sub sub = new Sub();

    assertEquals( "a:hi b:8", sub.func2( "hi", 8 ) );
    assertEquals( "a:hi b:8 c:9", sub.func3( "hi", 8, 9 ) );

    // test default value overrides as called from base class
    base = sub;

    assertEquals( "a:hi b:8", base.func2( "hi", 8 ) );
    assertEquals( "a:hi b:1", base.func2( "hi" ) ); // shows the default value override

    assertEquals( "a:hi b:8 c:16", base.func3( "hi", 8 ) );
    assertEquals( "a:hi b:2 c:4", base.func3( "hi" ) ); // shows the default value override
  }

  static class Base {
    String func( String a, int b = -1 ) { return "a:" + a + " b:" + b; }
    String func2( String a, int b = 0 ) { return "a:" + a + " b:" + b; }
    String func3( String a, int b = 0 ) { return "a:" + a + " b:" + b; }
  }

  static class Sub extends Base {
    @Override
    // add additional opt param `c`
    String func( String a, int b, int c = b ) { return "a:" + a + " b:" + b + " c:" + c; }

    @Override
    // override default value for `b`
    String func2( String a, int b = 1 ) { return "a:" + a + " b:" + b; }

    @Override
    // override default value for `b` and add additional opt param `c`
    String func3( String a, int b = 2, int c = b*2 ) { return "a:" + a + " b:" + b + " c:" + c; }
  }

  public void testOverrideOptParamValuesFromInterface()
  {
    // sub sanity check
    HasOptParamMethodImpl impl = new HasOptParamMethodImpl();

    assertEquals( "a:hi b:8", impl.func2( "hi", 8 ) );
    assertEquals( "a:hi b:8 c:9", impl.func3( "hi", 8, 9 ) );

    // test default value overrides as called from interface
    //noinspection UnnecessaryLocalVariable
    HasOptParamMethod iface = impl;

    assertEquals( "a:hi b:8", iface.func2( "hi", 8 ) );
    assertEquals( "a:hi b:1", iface.func2( "hi" ) ); // shows the default value override

    assertEquals( "a:hi b:8 c:16", iface.func3( "hi", 8 ) );
    assertEquals( "a:hi b:2 c:4", iface.func3( "hi" ) ); // shows the default value override
  }

  interface HasOptParamMethod
  {
    String func( String a, int b = 0 );
    String func2( String a, int b = 0 );
    String func3( String a, int b = 0 );
  }

  static class HasOptParamMethodImpl implements HasOptParamMethod {
    @Override
    // add additional opt param `c`
    public String func( String a, int b, int c = b ) { return "a:" + a + " b:" + b + " c:" + c; }

    @Override
    // override default value for `b`
    public String func2( String a, int b = 1 ) { return "a:" + a + " b:" + b; }

    @Override
    // override default value for `b` and add additional opt param `c`
    public String func3( String a, int b = 2, int c = b*2 ) { return "a:" + a + " b:" + b + " c:" + c; }
  }

  // this test demonstrates that handles overriding an opt params method having multiple opt
  // params and then only redefining one of the default values--shows that default values are indeed inherited.
  public void testDefaultsAreInherited()
  {
    Misc.Mid mid = new Misc.Mid();
    Misc.Top top = mid;

    String result = mid.sample();
    assertEquals( "hiya:hiya:hiya", result );

    result = top.sample();
    assertEquals( "hiya:hiya:hiya", result );

    result = top.sample( b:"hey" );
    assertEquals( "hiya:hey:hey", result );
  }
//  public void testDefaultsAreInherited3()
//  {
//    Misc.Mid2 mid = new Misc.Mid2();
//    Misc.Top top = mid;
//
//    String result = mid.sample();
//    assertEquals( "hiya:hiya:hiya", result );
//
//    result = top.sample();
//    assertEquals( "hiya:hiya:hiya", result );
//
//    result = top.sample( b:"hey" );
//    assertEquals( "hiya:hey:hey", result );
//  }

  public void testDefaultsAreInherited2()
  {
    Misc.Bot bot = new Misc.Bot();
    Misc.Mid mid = bot;
    Misc.Top top = bot;

    String result = bot.sample();
    assertEquals( "hi:hi:hi:hihi", result );

    result = mid.sample();
    assertEquals( "hi:hi:hi:hihi", result );

    result = top.sample();
    assertEquals( "hi:hi:hi:hihi", result );

    result = top.sample( b:"hey" );
    assertEquals( "hi:hey:hey:heyhey", result );
  }

  static class Misc
  {
    static class Top
    {
      String sample( String a="hello", String b=a )
      {
        return a + ":" + b;
      }
    }

    static class Mid extends Top
    {
      @Override // + extend with param c
      String sample( String a="hiya", String b, String c=b )
      {
        return a + ":" + b + ":" + c;
      }
    }

//    static class Mid2 extends Top
//    {
//      @Override // + extend with param c
//      String sample( String aa="hiya", String bb, String c=bb ) // tests that a compile error results due to param name mismatch w super
//      {
//        return aa + ":" + bb + ":" + c;
//      }
//    }

    static class Bot extends Mid
    {
      @Override // + extend with param d
      String sample( String a="hi", String b, String c, String d = c + c )
      {
        return a + ":" + b + ":" + c + ":" + d;
      }
    }
  }

  public void testSuperMethodOverriddenWithNoOptParams()
  {
    SuperMethodOverriddenWithNoOptParams.Base base = new SuperMethodOverriddenWithNoOptParams.Sub();
    String result = base.sample();
    assertEquals( "hello:::B", result );
  }

  static class SuperMethodOverriddenWithNoOptParams
  {
    static class Base
    {
      String sample( String a="hello", String b=a )
      {
        return a + ":" + b;
      }
    }

    static class Mid extends Base
    {
      @Override // no default overrides or extra params
      String sample( String a, String b )
      {
        return a + "::" + b;
      }
    }

    static class Sub extends Mid
    {
      @Override // override default for b, keep Top's default for a
      String sample( String a, String b="B" )
      {
        return a + ":::" + b;
      }
    }
  }
}