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

package manifold.ext.props;

import junit.framework.TestCase;
import manifold.ext.props.rt.api.get;
import manifold.ext.props.rt.api.prop;
import manifold.ext.props.rt.api.set;

public class PropsTest extends TestCase
{
  public void testProps()
  {
    FooSub fooSub = new FooSub( "hi", 3.14159 );
    assertEquals( "hi_sub", fooSub.thing );
    assertEquals( 6.28318D, fooSub.rate );
    fooSub.rate = 5.1;
    assertEquals( 10.2, fooSub.rate );

    // compound assign
    fooSub.rate += 5;
    assertEquals( 30.4, fooSub.rate );

    // inc/dec
//    fooSub.rate++;
//    assertEquals( 31.4, fooSub.rate );

    INamed named = new NamedInner( "scott" );
    assertEquals( "scott", named.name );
    named.name = "asdf";
    assertEquals( "asdf", named.name );

    IFoo foo = new FooImpl( "Bubby" );
    assertEquals( "Bubby", foo.name );
    foo.name = "Mo";
    assertEquals( "Mo", foo.name );
  }

  class NamedInner implements INamed
  {
    public @prop String name;

    public NamedInner( String name )
    {
      this.name = name;
    }
//
//    public String getName() {
//      return name;
//    }
//    public void setName(String value) {
//      name = value;
//    }
  }

  interface IFoo {
    @prop String name;
    default String getName()
    {
      return "hi";
    }
  }
  class FooImpl implements IFoo {
    @prop public String name;

    public FooImpl( String name )
    {
      this.name = name;
    }
  }
}
