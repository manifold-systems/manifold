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


public class ConstructorTest extends TestCase
{
  public void testConstructor()
  {
    MyRec myRec = new MyRec();
    assertEquals( "z", myRec._name );
    assertEquals( 33, myRec._age );

    myRec = new MyRec( "scott" );
    assertEquals( "scott", myRec._name );
    assertEquals( 999, myRec._age );

    myRec = new MyRecSub();
    assertEquals( "fred", myRec._name );
    assertEquals( 999, myRec._age );

    myRec.reset();
    assertEquals( "reset", myRec._name );
    assertEquals( 0, myRec._age );
  }

  public void testAnonCtor()
  {
    new MyRec( "", age:9 ) {};
  }

  interface Foo
  {
    void reset( String name="reset", int age=0 );
  }

  static class MyRec implements Foo
  {
    String _name;
    int _age;

    MyRec( String name, int age=999 )
    {
      _name = name;
      _age = age;
    }

    MyRec()
    {
      this(name:"z", age:33);
    }

    @Override
    public void reset( String name, int age )
    {
      _name = name;
      _age = age;
    }
  }

  static class MyRecSub extends MyRec
  {
    MyRecSub()
    {
      super( "fred" );
    }
  }
}