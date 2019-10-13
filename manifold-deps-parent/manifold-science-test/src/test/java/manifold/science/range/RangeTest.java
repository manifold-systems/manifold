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

package manifold.science.range;

import java.util.ArrayList;
import java.util.List;
import manifold.science.measures.Length;
import org.junit.Test;


import static manifold.science.util.UnitConstants.m;
import static manifold.collections.api.range.RangeFun.*;
import static org.junit.Assert.assertEquals;

public class RangeTest
{
  @Test
  public void testClosed()
  {
    int start = 3;
    List<Length> results = new ArrayList<>();
    for( Length l : start m to 10m )
    {
      int i = results.size();
      results.add( l );
      assertEquals( (i + start)m, l );
    }
    assertEquals( 8, results.size() );
  }

  @Test
  public void testLeftOpen()
  {
    int start = 3;
    List<Length> results = new ArrayList<>();
    for( Length l : start m _to 10m )
    {
      int i = results.size();
      results.add( l );
      assertEquals( (i + start + 1)m, l );
    }
    assertEquals( 7, results.size() );
  }

  @Test
  public void testRightOpen()
  {
    int start = 3;
    List<Length> results = new ArrayList<>();
    for( Length l : start m to_ 10m )
    {
      int i = results.size();
      results.add( l );
      assertEquals( (i + start)m, l );
    }
    assertEquals( 7, results.size() );
  }

  @Test
  public void testOpen()
  {
    int start = 3;
    List<Length> results = new ArrayList<>();
    for( Length l : start m _to_ 10m )
    {
      int i = results.size();
      results.add( l );
      assertEquals( (i + start + 1)m, l );
    }
    assertEquals( 6, results.size() );
  }
}
