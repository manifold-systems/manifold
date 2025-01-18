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
    assertEquals( "SubClass: BaseClass: Scott,100", baseClass.optionalParams( "Scott" ) );
    assertEquals( "SubClass: BaseClass: base", baseClass.testme( name:"base" ) );
    assertEquals( "SubClass: BaseClass: default", baseClass.testme() );
    assertEquals( "SubClass: BaseClass: base", baseClass.testmeDefaultImpl( name:"base" ) );
    assertEquals( "SubClass: BaseClass: default", baseClass.testmeDefaultImpl() );

    SubClass subClass = (SubClass)baseClass;
    assertEquals( "SubClass: BaseClass: Scott,90", subClass.optionalParams( name:"Scott", age:90) );
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
    String optionalParams( String name, int age )
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
    assertEquals( "a:hi b:0 c:0", base.func( "hi" ) );
  }

  static class Base {
    String func( String a, int b = 0 ) { return "a:" + a + " b:" + b; }
  }

  static class Sub extends Base {
//    @Override
    String func( String a, int b, int c = b ) { return "a:" + a + " b:" + b + " c:" + c; }
  }
}