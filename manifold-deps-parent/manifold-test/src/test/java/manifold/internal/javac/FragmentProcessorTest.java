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

package manifold.internal.javac;

import junit.framework.TestCase;

public class FragmentProcessorTest extends TestCase
{
  public void testFragmentParserLine()
  {
    FragmentProcessor fragmentProcessor = FragmentProcessor.instance();
    FragmentProcessor.Fragment fragment = fragmentProcessor.parseFragment( 0, "//[>MyType.graphql<]", HostKind.LINE_COMMENT );
    assertEquals( "MyType", fragment.getName() );
    assertEquals( "graphql", fragment.getExt() );
    assertNull( fragment.getScope() );
    fragment = fragmentProcessor.parseFragment( 0, "//[>MyType.graphql:myschema<]", HostKind.LINE_COMMENT );
    assertEquals( "MyType", fragment.getName() );
    assertEquals( "graphql", fragment.getExt() );
    assertEquals( "myschema", fragment.getScope() );
  }
}
