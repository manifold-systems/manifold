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

package manifold.ext.props.middle.auto.gen;

import manifold.ext.props.middle.auto.gen.GenSub;

import java.util.Collections;

public class UseGenSub
{
  static class Foo<T extends CharSequence> extends GenericBase<T>
  {
    public Foo( T tee )
    {
      super( tee );
    }

    @Override
    protected GenericBase<T> getMee()
    {
      return null;
    }

    @Override
    protected void setMee( GenericBase<T> mee )
    {

    }
  }
  public static void main( String[] args )
  {
    GenSub gs = new GenSub( "hi" );
    gs.tee = "foo";
    System.out.println(gs.tee);
    gs.list = Collections.singletonList( "bar" );
    System.out.println(gs.list.get(0).substring(0));
    gs.mee = gs;
    System.out.println(gs.mee + " : " + gs.tee);

    Foo<StringBuilder> foo = new Foo<>( new StringBuilder( "hi" ) );
    foo.tee = new StringBuilder( "foo" );
    System.out.println(foo.tee);
    foo.list = Collections.singletonList( new StringBuilder( "bar" ) );
    System.out.println(foo.list.get(0).substring(0));
    foo.mee = foo;
    System.out.println(foo.mee + " : " + foo.tee);
  }
}
