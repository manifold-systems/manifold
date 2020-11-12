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

package manifold.science.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.Test;


import static manifold.collections.api.range.RangeFun.*;
import static manifold.science.util.CoercionConstants.bd;
import static manifold.science.util.CoercionConstants.r;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BigDecimalTest
{
  @Test
  public void testBigDecimalOperators()
  {
    BigDecimal bd1 = new BigDecimal( "1.2" );
    BigDecimal bd2 = new BigDecimal( "2.4" );

    assertEquals( new BigDecimal( "-1.2" ), -bd1 );
    assertEquals( new BigDecimal( "3.6" ), bd1 + bd2 );
    assertEquals( new BigDecimal( "-1.2" ), bd1 - bd2 );
    assertEquals( new BigDecimal( "2.88" ), bd1 * bd2 );
    assertEquals( new BigDecimal( "0.5" ), bd1 / bd2 );
    assertEquals( new BigDecimal( "1.2" ), bd1 % bd2 );

    assertTrue( bd1 < bd2 );
    assertTrue( bd1 <= bd2 );
    assertTrue( bd2 > bd1 );
    assertTrue( bd2 >= bd1 );
    assertTrue( bd2 != bd1 );
    assertTrue( bd1 != bd2 );
    assertTrue( bd1 == new BigDecimal( "1.2" ) );
    assertTrue( new BigDecimal( "1.2" ) == bd1 );

    assertEquals( 5bd, 2bd + 3bd );
    assertEquals( -1bd, 2bd - 3bd );
    assertEquals( 6bd, 2bd * 3bd );
    assertEquals( 3bd, 6bd / 2bd );
    assertEquals( 1bd, 3bd % 2bd );
  }

  @Test
  public void testUnary()
  {
    BigDecimal x = 3bd;
    assertEquals( BigDecimal.valueOf( -3 ), -x );
    x = 3bd;
    assertEquals( BigDecimal.valueOf( 3 ), x-- );
    x = 3bd;
    assertEquals( BigDecimal.valueOf( 2 ), --x );
    x = 3bd;
    assertEquals( BigDecimal.valueOf( 3 ), x++ );
    x = 3bd;
    assertEquals( BigDecimal.valueOf( 4 ), ++x );
  }
  
  @Test
  public void testBigIntegerRange()
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
  public void testBigDecimalRange()
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
