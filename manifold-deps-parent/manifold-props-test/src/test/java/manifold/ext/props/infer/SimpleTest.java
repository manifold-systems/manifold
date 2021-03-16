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

package manifold.ext.props.infer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SimpleTest
{
  @Test
  public void testInnerAccess()
  {
    C c = new C();
    c.asdf();
  }

  static class A
  {
    private String getFoo()
    {
      return "foo";
    }
  }

//  static class B extends A
//  {
//    private void setFoo( String msxs )
//    {
//    }
//  }

  static class C
  {
    void asdf()
    {
      A a = new A();
      B b = new B();
      //a.foo = "";
      b.foo = "asdfx"; // B#setFoo(String) is package-private, this is accessible
//    String res = b.foo; // A#getFoo() is private, this is not accessible through B, should have "can't access write-only property", and does
    }

  }



  static interface ISomething
  {
    String getName();
    void setName(String name);
  }
  static interface ISomethingMore extends ISomething
  {
    String getName();
    void setName(String name);

    int getAge();
    void setAge(int age);
  }
  static class More implements ISomethingMore
  {

    private String _name;

    @Override
    public String getName()
    {
      return _name;
    }

    @Override
    public void setName( String name )
    {
      _name = name;
    }

    @Override
    public int getAge()
    {
      return 0;
    }

    @Override
    public void setAge( int age )
    {

    }
  }
  interface IBlah
  {
    ISomethingMore getMore();
    void setMore(ISomethingMore more);
  }
  static class BlahImpl implements IBlah
  {
    private ISomethingMore _more;

    @Override
    public ISomethingMore getMore()
    {
      return _more;
    }

    @Override
    public void setMore( ISomethingMore more )
    {
      _more = more;
    }
  }

  @Test
  public void testSomething()
  {
    IBlah blah = new BlahImpl();
    blah.more = new More();
    blah.more.name = "hi";
    assertEquals( "hi", blah.more.name );
    blah.more.age = 5;
  }

}