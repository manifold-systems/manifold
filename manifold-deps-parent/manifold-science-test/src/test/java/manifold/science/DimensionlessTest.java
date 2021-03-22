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

package manifold.science;

import manifold.science.util.DimensionlessConstants;
import org.junit.Test;

import static manifold.science.measures.CommonDimensionlessUnit.*;
import static org.junit.Assert.assertEquals;

public class DimensionlessTest
{
  @Test
  public void testDimensionlessConstantUnit()
  {
    assertEquals( 5 * DimensionlessConstants.pi, 5pi );
    assertEquals( 5 * DimensionlessConstants.phi, 5phi );
    assertEquals( 5 * DimensionlessConstants.kA, 5kA );
    assertEquals( 5 * DimensionlessConstants.mol, 5mol );
  }
}
