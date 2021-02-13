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

import manifold.ext.props.middle.NonbackingWithPropOptions;
import manifold.ext.props.middle.FromClassFile;

import junit.framework.TestCase;
import manifold.util.ReflectUtil;

public class NonbackingTest extends TestCase
{
  public void testNonbacking()
  {
    // field should not exist
    ReflectUtil.FieldRef nonbacking = ReflectUtil.field( FromClassFile.class, "nonbacking" );
    assertNull( nonbacking );

    // nonbacking field works
    FromClassFile fromClassFile = new FromClassFile();
    assertEquals( 8, fromClassFile.nonbacking );
    fromClassFile.nonbacking = 9;
    assertEquals( 9, fromClassFile.nonbacking );
  }

  public void testNonbackingWithPropOptions()
  {
    NonbackingWithPropOptions c = new NonbackingWithPropOptions();
    c.nonbackingFinal = "hello";
    assertEquals( "hi", c.nonbackingFinal );
  }
}
