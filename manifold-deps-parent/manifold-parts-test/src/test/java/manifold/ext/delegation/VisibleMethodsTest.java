/*
 * Copyright (c) 2023 - Manifold Systems LLC
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

package manifold.ext.delegation;

import junit.framework.TestCase;
import manifold.ext.delegation.rt.api.link;

public class VisibleMethodsTest extends TestCase
{
  public void testSyntheticMethodExclusion()
  {
    MyString s = new MyString( "hi" );
    assertEquals( "hi", s.toString() );
  }

  // CharSequence has a synthetic method, which should be excluded from processing
  static class MyString implements CharSequence {
    @link String string;

    public MyString(String string) {
      this.string = string;
    }
  }
}
