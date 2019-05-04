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

package manifold.api.host;

import abc.*;
import abc.sub.*;
import junit.framework.TestCase;

public class RelativePackageTest extends TestCase
{
  public void testSubPkgFromParentPkg() {
    Test1 test = Test1.builder().withSubTest(SubTest1.builder().withId("hi").build()).build();
    assertEquals( "hi", test.getSubTest().getId() );
  }

  public void testParentPkgFromSubPkg() {
    SubTest2 subtest = SubTest2.builder().withTest2(Test2.create()).build();
    assertNotNull( subtest.getTest2() );
  }
}
