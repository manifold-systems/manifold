/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.preprocessor;

#define AAA
#define BBB
#define CCC

#undef AAA

import org.junit.Test;


import static org.junit.Assert.*;


public class SimplePreprocessorTest
{
  @Test
  public void testSimple()
  {
    #if AAA
    String answer = "AAA";
    #elif BBB
    String answer = "BBB";
    #elif CCC
    String answer = "CCC";
    #else
    String answer = "DDD";
    #endif

    assertEquals( "BBB", answer );
  }

  @Test
  public void testNestedIf()
  {
  #if BBB
    #if AAA
    /*
    masked comment
     */
    // line comment 
    """
    text block
    """
    String answer = "AAA";
    #elif BBB
        #if AAA
    String answer = "AAA";
        #elif BBB
    String answer = "BBB"; // ding!
        #elif CCC
    String answer = "CCC";
        #else
    String answer = "DDD";
        #endif
    #elif CCC
    String answer = "CCC";
    #else
    String answer = "DDD";
    #endif
  #endif

    assertEquals( "BBB", answer );
  }

  @Test
  public void testNestedIf2()
  {
    assertEquals( "abcd", nested() );
  }

  @Test
  public void testEnvironmentDefinitions()
  {
    boolean success=false;
    #if JAVA_9_OR_LATER
    fail();
    #elif JAVA_8
    success = true;
    #else
    fail();
    #endif
    assertTrue(success);
  }

  @Test
  public void testBuildProps()
  {
    boolean success=false;
    #if NOT_MY_BUILD_PROP
    fail();
    #elif MY_BUILD_PROP
    success = true;
    #else
    fail();
    #endif
    assertTrue(success);
  }

  @Test
  public void testJavacOptionOverridesBuildProps()
  {
    boolean success=false;
    #if MY_PROP3 == 123
    fail();
    #elif MY_PROP3 == 2
    success = true;
    #else
    fail();
    #endif
    assertTrue(success);
  }

  @Test
  public void testMethodBoundary()
  {
    #if BBB
    String hello = "hi";
    #else comment out the method 's closing brace
  }
    #endif
  }

  @Test
  public void testOptionalClass()
  {
    assertEquals( "hi", new OptionalClass().foo() );
  }

  @Test
  public void testOperatorOverloadingNotEnabled()
  {
    short a = 0;
    // tests that visitAssignop() is not enabled in ParseProcessor, otherwise this fails if manifold-ext is not enabled
    // (it transforms what ParseProcessor does to be legal)
    a += (byte)1;
    assertEquals( 1, a );
  }
  
  private String nested()
  {
    #if BBB
      StringBuilder sb = new StringBuilder();
      #if BBB
        sb.append("a");
      #else
        #error "boom"
      #endif
        sb.append("b");
      #if AAA
        sb.append("wrong");
      #elif BBB
        sb.append("c");
      #endif
      sb.append("d");
    #elif CCC
      sb.append("double dog wrong");
    #endif
    return sb.toString();
  }
}
