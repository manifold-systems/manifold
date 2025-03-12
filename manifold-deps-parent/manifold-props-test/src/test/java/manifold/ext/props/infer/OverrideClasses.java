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

public class OverrideClasses
{
  private String outer = "Enclosing";

  String getOuter()
  {
    return "Outer";
  }

  static class Parent
  {
    protected String foo = "Parent";
    /*package*/ String bar = "Parent";
    private String baz = "Parent";

    String getFoo()
    {
      return foo;
    }

    String getBar()
    {
      return bar;
    }

    String getBaz()
    {
      return baz;
    }
  }

  static Child newChild()
  {
    return new OverrideClasses().new Child();
  }

  class Child extends Parent
  {
    @Override
    String getFoo()
    {
      if( foo.equals( "Parent" ) )
      {
        foo = "Child"; // assign directly to field bc it's accessible
      }
      return foo;
    }

    @Override
    String getBar()
    {
      if( bar.equals( "Parent" ) )
      {
        bar = "Child"; // assign directly to field bc it's accessible
      }
      return bar;
    }

    @Override
    String getBaz()
    {
      return "Child";
    }

//    String getBlad()
//    {
//      return outer;
//    }
  }
}