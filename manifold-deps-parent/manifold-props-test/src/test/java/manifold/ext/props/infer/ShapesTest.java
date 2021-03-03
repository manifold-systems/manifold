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

import manifold.ext.props.middle.auto.RightTriangle;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ShapesTest
{
  @Test
  public void testShapes()
  {
    RightTriangle t = new RightTriangle( 3, 4 );

    assertEquals( 3d, t.a, 0 );
    assertEquals( 4d, t.b, 0 );
    assertEquals( 5d, t.c, 0 );
    assertEquals( 6d, t.area, 0 );
    assertEquals( 5d, t.area(), 0 ); // tests conflicting field
    assertEquals( 6d, t.getArea(), 0 );
    t.color = "leopardskin";
    assertEquals( "leopardskin", t.color );
  }
}
