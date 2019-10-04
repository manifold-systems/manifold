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

package manifold.collections.api;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.Test;


import static manifold.collections.api.range.RangeFun.to;
import static manifold.collections.api.range.RangeFun.step;
import static manifold.collections.api.range.RangeFun.inside;
import static manifold.collections.api.range.RangeFun.outside;
import static org.junit.Assert.*;

public class RangeFunTest
{
  @Test
  public void testInt()
  {
    int check = 1;
    for( int i: 1 to 10 )
    {
      assertEquals( check++, i );
    }
    assertEquals( 11, check );

    check = 1;
    for( int i: 1 to 10 step 2 )
    {
      assertEquals( check, i );
      check += 2;
    }
    assertEquals( 11, check );

    assertTrue( 5 inside 2 to 10 );
    assertTrue( 5 inside 10 to 2 );

    assertFalse( 5 inside 6 to 10 );
    assertFalse( 5 inside 10 to 6 );
    
    assertTrue( 5 outside 6 to 10 );
    assertTrue( 5 outside 10 to 6 );

    int left = -6;
    int right = 10;
    assertFalse( 5 outside left to right );
    assertFalse( 5 outside right to left );
  }

  @Test
  public void testLong()
  {
    long lcheck = 1;
    for( long l: 1l to 10l )
    {
      assertEquals( lcheck++, l );
    }
    assertEquals( 11, lcheck );
  }

  @Test
  public void testDouble()
  {
    double dcheck = 1;
    for( double d: 1d to 10d )
    {
      assertEquals( dcheck++, d, 0 );
    }
    assertEquals( dcheck, 11, 0 );
  }

  @Test
  public void testBigInteger()
  {
    BigInteger bi2 = BigInteger.valueOf( 2 );
    BigInteger bi10 = BigInteger.valueOf( 10 );
    BigInteger biCheck = bi2;
    for( BigInteger value: bi2 to bi10 )
    {
      assertEquals( biCheck, value );
      biCheck = biCheck + BigInteger.ONE;
    }
    assertEquals( bi10+ BigInteger.ONE, biCheck );
  }

  @Test
  public void testBigDecimal()
  {
    BigDecimal bd2 = BigDecimal.valueOf( 2 );
    BigDecimal bd10 = BigDecimal.valueOf( 10 );
    BigDecimal bdCheck = bd2;
    for( BigDecimal value: bd2 to bd10 )
    {
      assertEquals( bdCheck, value );
      bdCheck = bdCheck + BigDecimal.ONE;
    }
    assertEquals( bd10 + BigDecimal.ONE, bdCheck );
  }
}
