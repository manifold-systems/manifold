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

import java.util.Arrays;
import java.util.List;

public class StaticVarArgsObjectExtMethodTest extends TestCase
{
  public void testStaticVarArgsObjectExtMethod()
  {
    // from MyObjectExt
    List<Integer> result = myStaticVarArgsMethod( 1, 2, 3, 4 );
    assertEquals( Arrays.asList( 1, 2, 3, 4 ), result );
  }
}
