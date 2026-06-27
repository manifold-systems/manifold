/*
 * Copyright (c) 2023 - Manifold Systems LLC
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

package manifold.ext.delegation.parts.inheritance;

import junit.framework.TestCase;
import manifold.ext.delegation.rt.api.link;
import manifold.ext.delegation.rt.api.part;

public class InheritanceTest extends TestCase
{
  public void testInheritance()
  {
    MyA a = new MyA();
    assertEquals( "x_x_y_z", a.a( "x_" ) );
  }

  interface A
  {
    String a( String a );
    String b( String b );
  }

  static @part class AImpl implements A
  {
    @Override
    public String a( String a )
    {
      return a + b( a );
    }

    @Override
    public String b( String b )
    {
      return b;
    }
  }

  static @part class SubAImpl extends AImpl
  {
  }

  static class MyA implements A
  {
    @link A a = new SubAImpl();

    @Override
    public String b( String b )
    {
      return a.b( b ) + "y_z";
    }
  }
}
