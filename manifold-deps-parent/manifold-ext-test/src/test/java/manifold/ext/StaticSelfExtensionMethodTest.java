/*
 * Copyright (c) 2022 - Manifold Systems LLC
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

package manifold.ext;

import junit.framework.TestCase;

public class StaticSelfExtensionMethodTest extends TestCase
{
  public void testStaticSelfMethod()
  {
    // from MyObjectExt#myStaticSelfMethod
    String result = String.myStaticSelfMethod( s -> s.length() > 0 );
  }

  public void testThisClassMethod_FromStatic() throws Exception
  {
    Class result = String.mySmartStaticSelfMethod( s -> s.isEmpty() );
    assertSame( String.class, result );
    result = StringBuilder.mySmartStaticSelfMethod( s -> s.append( 's' ).length() == 1 );
    assertSame( StringBuilder.class, result );
  }

  public void testThisClassMethod_FromInstance() throws Exception
  {
    Class result = "hi".mySmartStaticSelfMethod( s -> s.isEmpty() );
    assertSame( String.class, result );
  }

  public void testThisClassMethod_Unqualified() throws Exception
  {
    MyClass.testThisClassMethod_Unqualified();
  }

  public static class MyClass
  {
    static void testThisClassMethod_Unqualified() throws Exception
    {
      Class result = mySmartStaticSelfMethod( thisClass -> thisClass.foo() );
      assertSame( MyClass.class, result );
    }

    private boolean foo()
    {
      return true;
    }
  }
}
