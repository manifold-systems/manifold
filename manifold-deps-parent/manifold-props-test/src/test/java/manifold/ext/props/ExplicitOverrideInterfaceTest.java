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
import manifold.ext.props.rt.api.override;
import manifold.ext.props.rt.api.val;
import manifold.ext.props.rt.api.var;

@SuppressWarnings( "JavaReflectionMemberAccess" )
public class ExplicitOverrideInterfaceTest extends TestCase
{
  public void testExplicitOverrideInterface()
  {
    DerivedWithDefault derivedWithDefault = new DerivedWithDefault() {};
    assertEquals( "hi", derivedWithDefault.prop );
  }

  public void testExplicitClassVarOverrideInterface()
  {
    DerivedClassVar derivedClassVar = new DerivedClassVar();
    derivedClassVar.prop = "hey";
    assertEquals( "hey", derivedClassVar.prop );
  }

  public void testExplicitClassValOverrideInterface()
  {
    DerivedClassVal derivedClassVal = new DerivedClassVal();
    assertEquals( "val", derivedClassVal.prop );
  }

  public void testExplicitClassOverrideInterfaceWithDefault()
  {
    DerivedClass2 derivedClass2 = new DerivedClass2();
    assertEquals( "hi", derivedClass2.prop );
    derivedClass2.prop = "hey";
    assertEquals( "hey", derivedClass2.prop );
  }

  interface Base
  {
    @val String prop;
  }

  interface Derived extends Base
  {
    @override @val String prop;
  }

  interface DerivedWithDefault extends Derived
  {
    @override @val String prop = "hi";
  }

  static class DerivedClassVal implements Base
  {
    @override @val String prop = "val";
  }

  static class DerivedClassVar implements Base
  {
    @override @var String prop;
  }

  static class DerivedClass2 implements DerivedWithDefault
  {
    @override @var String prop = DerivedWithDefault.super.getProp(); // todo: DerivedWithDefault.super.prop 
  }
}
