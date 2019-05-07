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

package manifold.templates.directives;

import directives.nest.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class NestTest
{
  @Test
  public void testNesting()
  {
    assertEquals(
      "abc\n" +
      "  foo\n" +
      "    hubba\n" +
      "    bubba hi\n" +
      "    bubble\n" +
      "  baz\n" +
      "def",
      Outer.render() );
  }

  @Test
  public void basicNestWorks()
  {
    assertEquals( "15", SimpleNest.render() );
  }

  @Test
  public void NestWithParamsWorks()
  {
    assertEquals( "Carson", NestWithParams.render() );
  }

  @Test
  public void NestWithMultipleParamsWorks()
  {
    assertEquals( "Name:CarsonAge:2000", NestWithMultipleParams.render() );
  }

  @Test
  public void conditionalNestWithParamsWorks()
  {
    assertEquals( "Carson", ConditionalNestWithParams.render( true ) );
    assertEquals( "", ConditionalNestWithParams.render( false ) );
  }

  @Test
  public void conditionalNestWithoutParamsWorks()
  {
    assertEquals( "15", SimpleConditionalNest.render( true ) );
    assertEquals( "", SimpleConditionalNest.render( false ) );
  }
}
