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


public class ParamRefTest extends TestCase
{
  public void testParamRef()
  {
    Foo foo = new Foo();
    assertEquals( "a:hi b:1 c:1", foo.func("hi") );
    assertEquals( "a:hi b:1 c:1", foo.func(a:"hi") );
    assertEquals( "a:hi b:2 c:2", foo.func("hi", 2) );
    assertEquals( "a:hi b:2 c:2", foo.func(a:"hi", b:2) );
    assertEquals( "a:hi b:2 c:2", foo.func("hi", b:2) );
    assertEquals( "a:hi b:1 c:3", foo.func("hi", c:3) );
    assertEquals( "a:hi b:2 c:3", foo.func("hi", 2, 3) );
    assertEquals( "a:hi b:2 c:3", foo.func(a:"hi", b:2, c:3) );
  }

  static class Foo
  {
    String func( String a, int b = 1, int c = b ) { return "a:" + a + " b:" + b + " c:" + c; }
  }
}