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
}