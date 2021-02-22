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
import manifold.ext.props.rt.api.PropOption;
import manifold.ext.props.rt.api.override;
import manifold.ext.props.rt.api.val;
import manifold.ext.props.rt.api.var;

import java.util.Arrays;
import java.util.List;

import static manifold.ext.props.rt.api.PropOption.Final;

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

    IFoo foo = new FooImpl( "Bubby", Arrays.asList( "red", "blue", "green" ) );
    assertEquals( "Bubby", foo.name );
    ((FooImpl)foo).name = "Mo";
    assertEquals( "Mo", foo.name );
    assertEquals( Arrays.asList( "red", "blue", "green" ), foo.colors );
  }

  class NamedInner implements INamed
  {
    @override @var String name;

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
    @val String name = "hi";  // generates a 'default' getter returning "hi"
    @var List<String> colors;
  }
  class FooImpl implements IFoo {
    @override @var(Final) String name;
    @override @var List<String> colors;
    @var List<String> things;
    @var(Final) String whatever;
    @val final int finalInt;
    @val int readonlyInt;

    public FooImpl( String name, List<String> colors )
    {
      this.name = name;
      this.colors = colors;

      this.finalInt = 8; // init final var
      this.readonlyInt = 9; // init read-only var
      readonlyInt = 10; // can init read-only more than once in constructor
    }

    public final String getWhatever()
    {
      return whatever;
    }
    public final void setWhatever( String value )
    {
      whatever = value;
    }

//    public void setFinalInt( int value )
//    {
////      finalInt = value;
//    }
//    // should cause error because no @set defined for readonlyInt property
//    public void setReadonlyInt( int value )
//    {
//      readonlyInt = value;
//    }

    public void setThings( List<? extends CharSequence> things )
    {
      this.things = (List<String>)things;
    }
  }
}
